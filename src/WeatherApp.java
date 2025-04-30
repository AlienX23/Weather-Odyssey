import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WeatherApp is a JavaFX application that fetches and displays weather data for a city
 * using the OpenWeatherMap API. It includes features like current weather, a 5-day forecast,
 * unit conversion, search history, and a dynamic UI with weather-based backgrounds.
 *
 * @author [Your Name]
 * @version 1.5
 * @since March 27, 2025
 */
public class WeatherApp extends Application {

    // API constants for OpenWeatherMap
    private static final String API_KEY = "dd360c20e1400a22e10af9260d9a33f5"; // OpenWeatherMap API key
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather?"; // Base URL for current weather
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast?"; // Base URL for 5-day forecast
    private static final String ICON_URL = "https://openweathermap.org/img/wn/"; // Base URL for weather icons

    // UI components for displaying weather data
    private Label weatherLabel; // Displays the current temperature
    private Label humidityLabel; // Displays the current humidity
    private Label windLabel; // Displays the current wind speed
    private Label conditionLabel; // Displays the current weather condition (e.g., "clear sky")
    private Label errorLabel; // Displays error messages (e.g., "Invalid location!")
    private ImageView weatherIcon; // Displays the icon for the current weather condition

    // Input fields for user interaction
    private TextField cityField; // Text field for entering the city name
    private ComboBox<String> unitComboBox; // Dropdown for selecting temperature unit (Celsius/Fahrenheit)

    // Search history components
    private ListView<String> historyListView; // Displays the list of recent searches
    private List<String> searchHistory = new ArrayList<>(); // Stores search history entries

    // State variables for managing app behavior
    private String currentCondition = "clear"; // Tracks the current weather condition for dynamic background
    private ProgressIndicator loadingIndicator; // Shows a loading spinner during API requests
    private String lastSearchedCity = ""; // Tracks the last successfully searched city
    private String currentUnits = "metric"; // Tracks the current unit for display ("metric" or "imperial")
    private String fetchedUnits = "metric"; // Tracks the unit in which data was fetched
    private JSONObject lastWeatherData; // Stores the last fetched weather data for caching
    private JSONObject lastForecastData; // Stores the last fetched forecast data for caching
    private boolean lastSearchSuccessful = false; // Tracks whether the last search was successful

    // Forecast display components
    private HBox forecastBox; // Horizontal box to display 5-day forecast
    private List<ForecastDayBox> forecastDayBoxes = new ArrayList<>(); // Stores forecast boxes for reuse

    // Cache for weather icons to improve performance
    private Map<String, Image> iconCache = new HashMap<>(); // Maps icon codes to Image objects

    /**
     * A helper class to store the UI components of a single forecast day, allowing reuse
     * when updating temperatures without rebuilding the entire forecast display.
     */
    private static class ForecastDayBox {
        VBox box; // The container for the forecast day's UI
        Label tempLabel; // Label for the day's temperature
        ImageView icon; // Icon for the day's weather condition

        ForecastDayBox(VBox box, Label tempLabel, ImageView icon) {
            this.box = box;
            this.tempLabel = tempLabel;
            this.icon = icon;
        }
    }

    /**
     * Sets up the JavaFX UI, including input fields, weather display, forecast, and search history.
     *
     * @param primaryStage The main stage (window) of the JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        // Create the main layout (a vertical box) to hold all UI components
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);

        // Set the initial dynamic background based on the default weather condition
        setDynamicBackground(root);

        // Input section for city name and unit selection
        HBox inputBox = new HBox(15);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(10));
        inputBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 10;");

        // Text field for entering the city name
        cityField = new TextField();
        cityField.setPromptText("Enter city name");
        cityField.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 5;");
        cityField.setPrefWidth(200);
        Tooltip cityTooltip = new Tooltip("Enter the name of a city to fetch weather data (e.g., London)");
        cityField.setTooltip(cityTooltip);

        // Dropdown for selecting temperature unit (Celsius or Fahrenheit)
        unitComboBox = new ComboBox<>();
        unitComboBox.getItems().addAll("Celsius", "Fahrenheit");
        unitComboBox.setValue("Celsius");
        unitComboBox.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 5;");
        Tooltip unitTooltip = new Tooltip("Select temperature unit: Celsius or Fahrenheit");
        unitComboBox.setTooltip(unitTooltip);
        // Listener to update weather data when the unit changes
        unitComboBox.setOnAction(e -> {
            if (lastSearchedCity.isEmpty()) return; // No city searched yet
            currentUnits = unitComboBox.getValue().equals("Celsius") ? "metric" : "imperial";
            if (lastSearchSuccessful) {
                updateTemperatureAndWindDisplay(lastWeatherData, lastForecastData);
            }
        });

        // Search button to fetch weather data
        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #FF6F61; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        searchButton.setOnMouseEntered(e -> searchButton.setStyle("-fx-background-color: #FF8A80; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;"));
        searchButton.setOnMouseExited(e -> searchButton.setStyle("-fx-background-color: #FF6F61; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;"));
        searchButton.setOnAction(e -> fetchWeatherData());
        Tooltip searchTooltip = new Tooltip("Click to fetch weather data for the entered city");
        searchButton.setTooltip(searchTooltip);

        // Refresh button to re-fetch data for the last searched city
        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle("-fx-background-color: #42A5F5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;"));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;"));
        refreshButton.setOnAction(e -> {
            if (!lastSearchedCity.isEmpty()) {
                cityField.setText(lastSearchedCity);
                fetchWeatherData();
            } else {
                showError("No city to refresh! Please search for a city first.");
            }
        });
        Tooltip refreshTooltip = new Tooltip("Click to refresh weather data for the last searched city");
        refreshButton.setTooltip(refreshTooltip);

        // Loading indicator to show while fetching data
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(30, 30);

        // Add all input components to the input box
        inputBox.getChildren().addAll(cityField, unitComboBox, searchButton, refreshButton, loadingIndicator);

        // Weather display section
        VBox weatherBox = new VBox(10);
        weatherBox.setAlignment(Pos.CENTER);
        weatherBox.setPadding(new Insets(15));
        weatherBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15; -fx-border-color: #2196F3; -fx-border-radius: 15;");
        weatherBox.setEffect(new DropShadow(10, Color.GRAY));

        weatherLabel = new Label("Temperature: --");
        weatherLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        humidityLabel = new Label("Humidity: --");
        humidityLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");
        windLabel = new Label("Wind Speed: --");
        windLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");
        conditionLabel = new Label("Conditions: --");
        conditionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");
        weatherIcon = new ImageView();
        weatherIcon.setFitHeight(120);
        weatherIcon.setFitWidth(120);
        weatherBox.getChildren().addAll(weatherLabel, humidityLabel, windLabel, conditionLabel, weatherIcon);

        // Forecast display section
        VBox forecastContainer = new VBox(5);
        forecastContainer.setPadding(new Insets(10));
        forecastContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10; -fx-border-color: #2196F3; -fx-border-radius: 10;");
        forecastContainer.setEffect(new DropShadow(5, Color.GRAY));

        Label forecastTitle = new Label("5-Day Forecast");
        forecastTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333; -fx-font-weight: bold;");

        forecastBox = new HBox(10);
        forecastBox.setAlignment(Pos.CENTER);
        ScrollPane forecastScroll = new ScrollPane(forecastBox);
        forecastScroll.setFitToHeight(true);
        forecastScroll.setPrefHeight(150);
        forecastScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        forecastContainer.getChildren().addAll(forecastTitle, forecastScroll);

        // Search history section
        VBox historyBox = new VBox(5);
        historyBox.setPadding(new Insets(10));
        historyBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10; -fx-border-color: #2196F3; -fx-border-radius: 10;");
        historyBox.setEffect(new DropShadow(5, Color.GRAY));

        HBox historyHeader = new HBox(10);
        historyHeader.setAlignment(Pos.CENTER_LEFT);
        Label historyLabel = new Label("Search History");
        historyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        Button clearHistoryButton = new Button("Clear History");
        clearHistoryButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 5;");
        clearHistoryButton.setOnMouseEntered(e -> clearHistoryButton.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 5;"));
        clearHistoryButton.setOnMouseExited(e -> clearHistoryButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 5;"));
        clearHistoryButton.setOnAction(e -> {
            searchHistory.clear();
            historyListView.getItems().clear();
        });
        historyHeader.getChildren().addAll(historyLabel, clearHistoryButton);

        historyListView = new ListView<>();
        historyListView.setPrefHeight(120);
        historyListView.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-text-fill: #555555;");
        historyBox.getChildren().addAll(historyHeader, historyListView);

        // Error message label
        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Add all components to the root layout
        root.getChildren().addAll(inputBox, weatherBox, forecastContainer, historyBox, errorLabel);

        // Set up the scene and stage
        Scene scene = new Scene(root, 600, 500); // Reduced width since coordinate fields are removed
        primaryStage.setTitle("Weather Odyssey");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Sets the background gradient of the app based on the current weather condition.
     * For example, clear skies use a yellow gradient, while rain uses a blue gradient.
     *
     * @param root The main layout (VBox) to apply the background to
     */
    private void    setDynamicBackground(VBox root) {
        LinearGradient gradient;

        // Choose a gradient based on the current weather condition
        switch (currentCondition.toLowerCase()) {
            case "clear":
                gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#FFD54F")), new Stop(1, Color.web("#FFB300")));
                break;
            case "clouds":
            case "scattered clouds":
            case "broken clouds":
            case "overcast clouds":
                gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#B0BEC5")), new Stop(1, Color.web("#78909C")));
                break;
            case "rain":
            case "light rain":
            case "shower rain":
                gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#4FC3F7")), new Stop(1, Color.web("#0288D1")));
                break;
            case "thunderstorm":
                gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#546E7A")), new Stop(1, Color.web("#263238")));
                break;
            default: // Default to a neutral blue gradient
                gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#4FC3F7")), new Stop(1, Color.web("#0288D1")));
                break;
        }

        root.setBackground(new Background(new BackgroundFill(gradient, null, null)));
    }

    /**
     * Fetches weather data for the entered city using the OpenWeatherMap API.
     * The data is fetched in a separate thread to keep the UI responsive.
     * Handles various error cases with specific error messages displayed on the GUI.
     */
    private void fetchWeatherData() {
        String location = cityField.getText().trim();

        // Validate input: city name must be provided
        if (location.isEmpty()) {
            showError("Please enter a city name!");
            return;
        }

        currentUnits = unitComboBox.getValue().equals("Celsius") ? "metric" : "imperial";

        // Check if we can use cached data (same city and units, and last search was successful)
        if (location.equals(lastSearchedCity) && currentUnits.equals(fetchedUnits) && lastSearchSuccessful && lastWeatherData != null && lastForecastData != null) {
            updateWeatherDisplay(lastWeatherData, lastForecastData);
            return;
        }

        // Show loading indicator while fetching data
        loadingIndicator.setVisible(true);
        errorLabel.setText("");

        // Fetch data in a separate thread to avoid blocking the UI
        new Thread(() -> {
            try {
                // Build the API URL using the city name
                String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8.toString());
                String urlString = API_URL + "q=" + encodedLocation + "&units=" + currentUnits + "&appid=" + API_KEY;
                String forecastUrl = FORECAST_URL + "q=" + encodedLocation + "&units=" + currentUnits + "&appid=" + API_KEY;

                // Fetch current weather data
                JSONObject weatherData = getJsonFromUrl(urlString);

                // Check for API error response (e.g., city not found)
                if (weatherData.has("cod") && weatherData.getInt("cod") != 200) {
                    String errorMessage = weatherData.has("message") ? weatherData.getString("message") : "Unknown error";
                    javafx.application.Platform.runLater(() -> {
                        showError("Invalid location: The city could not be found. Please try again. (" + errorMessage + ")");
                        loadingIndicator.setVisible(false);
                        lastSearchSuccessful = false; // Mark the search as unsuccessful
                    });
                    return;
                }

                // Fetch 5-day forecast data
                JSONObject forecastData = getJsonFromUrl(forecastUrl);

                // Update the UI on the JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    // Update last searched city and data only on success
                    lastSearchedCity = location;
                    fetchedUnits = currentUnits;
                    lastWeatherData = weatherData;
                    lastForecastData = forecastData;
                    lastSearchSuccessful = true; // Mark the search as successful

                    updateWeatherDisplay(weatherData, forecastData);

                    // Update search history with city name (only for successful searches)
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String historyEntry = location + " - " + timestamp;
                    if (!searchHistory.contains(historyEntry)) {
                        searchHistory.add(0, historyEntry);
                        historyListView.getItems().clear();
                        historyListView.getItems().addAll(searchHistory);
                    }

                    // Hide loading indicator
                    loadingIndicator.setVisible(false);
                });

            } catch (Exception e) {
                // Handle network or API errors
                javafx.application.Platform.runLater(() -> {
                    String errorMessage;
                    if (e.getMessage().contains("Server returned HTTP response code")) {
                        // Extract the HTTP status code from the exception message
                        String[] parts = e.getMessage().split(" ");
                        String statusCode = parts.length > 4 ? parts[4] : "Unknown";
                        switch (statusCode) {
                            case "400":
                                errorMessage = "API error: Bad request. Please check your input.";
                                break;
                            case "401":
                                errorMessage = "API error: Invalid API key. Please check your configuration.";
                                break;
                            case "429":
                                errorMessage = "API error: Too many requests. Please try again later.";
                                break;
                            case "500":
                            case "503":
                                errorMessage = "API error: Server error. Please try again later.";
                                break;
                            default:
                                errorMessage = "Network error: Unable to fetch weather data. Please check your internet connection.";
                                break;
                        }
                    } else {
                        errorMessage = "Error fetching weather data: " + e.getMessage();
                    }
                    showError(errorMessage);
                    loadingIndicator.setVisible(false);
                    lastSearchSuccessful = false; // Mark the search as unsuccessful
                });
            }
        }).start();
    }

    /**
     * Converts a temperature value between Celsius and Fahrenheit.
     *
     * @param temp The temperature value to convert
     * @param fromUnit The unit of the input temperature ("metric" for Celsius, "imperial" for Fahrenheit)
     * @param toUnit The unit to convert to ("metric" for Celsius, "imperial" for Fahrenheit)
     * @return The converted temperature value
     */
    private double convertTemperature(double temp, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) return temp;
        if (fromUnit.equals("metric") && toUnit.equals("imperial")) {
            // Celsius to Fahrenheit: °F = (°C * 9/5) + 32
            return (temp * 9.0 / 5.0) + 32;
        } else if (fromUnit.equals("imperial") && toUnit.equals("metric")) {
            // Fahrenheit to Celsius: °C = (°F - 32) * 5/9
            return (temp - 32) * 5.0 / 9.0;
        }
        return temp; // No conversion needed if units are the same
    }

    /**
     * Converts a wind speed value between metric (m/s) and imperial (mph) units.
     *
     * @param speed The wind speed value to convert
     * @param fromUnit The unit of the input speed ("metric" for m/s, "imperial" for mph)
     * @param toUnit The unit to convert to ("metric" for m/s, "imperial" for mph)
     * @return The converted wind speed value
     */
    private double convertWindSpeed(double speed, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) return speed;
        if (fromUnit.equals("metric") && toUnit.equals("imperial")) {
            // m/s to mph: mph = m/s * 2.23694
            return speed * 2.23694;
        } else if (fromUnit.equals("imperial") && toUnit.equals("metric")) {
            // mph to m/s: m/s = mph / 2.23694
            return speed / 2.23694;
        }
        return speed; // No conversion needed if units are the same
    }

    /**
     * Updates the entire UI with new weather and forecast data, including icons, conditions,
     * and forecast boxes. This method is called when new data is fetched (e.g., new city or refresh).
     *
     * @param weatherData The JSON object containing current weather data
     * @param forecastData The JSON object containing 5-day forecast data
     */
    private void updateWeatherDisplay(JSONObject weatherData, JSONObject forecastData) {
        try {
            // Extract current weather data from the JSON response
            String conditions = weatherData.getJSONArray("weather").getJSONObject(0).getString("description");
            String iconCode = weatherData.getJSONArray("weather").getJSONObject(0).getString("icon");
            double temp = weatherData.getJSONObject("main").getDouble("temp");
            double humidity = weatherData.getJSONObject("main").getDouble("humidity");
            double windSpeed = weatherData.getJSONObject("wind").getDouble("speed");

            // Update the background based on the current weather condition
            currentCondition = conditions;
            setDynamicBackground((VBox) weatherLabel.getParent().getParent());

            // Update the weather icon (load asynchronously and cache)
            if (!iconCache.containsKey(iconCode)) {
                iconCache.put(iconCode, new Image(ICON_URL + iconCode + "@2x.png", true)); // true for background loading
            }
            weatherIcon.setImage(iconCache.get(iconCode));

            // Update forecast boxes (only if forecast data has changed)
            forecastBox.getChildren().clear();
            forecastDayBoxes.clear();
            for (int i = 0; i < forecastData.getJSONArray("list").length(); i += 8) { // Every 24 hours (3-hour intervals, so 8 * 3 = 24 hours)
                JSONObject day = forecastData.getJSONArray("list").getJSONObject(i);
                String date = day.getString("dt_txt").split(" ")[0];
                double dayTemp = day.getJSONObject("main").getDouble("temp");
                String dayCondition = day.getJSONArray("weather").getJSONObject(0).getString("description");
                String dayIconCode = day.getJSONArray("weather").getJSONObject(0).getString("icon");

                // Create a forecast box for each day
                VBox dayBox = new VBox(5);
                dayBox.setAlignment(Pos.CENTER);
                dayBox.setPadding(new Insets(10));
                dayBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #2196F3; -fx-border-radius: 10;");
                dayBox.setEffect(new DropShadow(5, Color.LIGHTGRAY));

                Label dateLabel = new Label(date);
                dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold;");
                ImageView dayIcon = new ImageView();
                dayIcon.setFitHeight(50);
                dayIcon.setFitWidth(50);
                // Load forecast icon asynchronously and cache
                if (!iconCache.containsKey(dayIconCode)) {
                    iconCache.put(dayIconCode, new Image(ICON_URL + dayIconCode + "@2x.png", true));
                }
                dayIcon.setImage(iconCache.get(dayIconCode));

                Label tempLabel = new Label(); // Will be updated in updateTemperatureAndWindDisplay
                tempLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
                Label conditionLabel = new Label(dayCondition);
                conditionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

                dayBox.getChildren().addAll(dateLabel, dayIcon, tempLabel, conditionLabel);
                forecastBox.getChildren().add(dayBox);
                forecastDayBoxes.add(new ForecastDayBox(dayBox, tempLabel, dayIcon));
            }

            // Update temperature and wind speed labels
            updateTemperatureAndWindDisplay(weatherData, forecastData);

        } catch (Exception e) {
            showError("Error updating weather display: " + e.getMessage());
            lastSearchSuccessful = false; // Mark the search as unsuccessful
        }
    }

    /**
     * Updates only the temperature and wind speed labels for the current weather and forecast.
     * This method is called when the unit changes (e.g., Celsius to Fahrenheit) to avoid
     * rebuilding the entire UI.
     *
     * @param weatherData The JSON object containing current weather data
     * @param forecastData The JSON object containing 5-day forecast data
     */
    private void updateTemperatureAndWindDisplay(JSONObject weatherData, JSONObject forecastData) {
        String tempUnit = unitComboBox.getValue().equals("Celsius") ? "°C" : "°F";
        String speedUnit = unitComboBox.getValue().equals("Celsius") ? "m/s" : "mph";

        try {
            // Extract current weather data
            double temp = weatherData.getJSONObject("main").getDouble("temp");
            double humidity = weatherData.getJSONObject("main").getDouble("humidity");
            double windSpeed = weatherData.getJSONObject("wind").getDouble("speed");

            // Convert temperature and wind speed if necessary
            double displayTemp = convertTemperature(temp, fetchedUnits, currentUnits);
            double displayWindSpeed = convertWindSpeed(windSpeed, fetchedUnits, currentUnits);

            // Update current weather labels
            weatherLabel.setText(String.format("Temperature: %.1f%s", displayTemp, tempUnit));
            humidityLabel.setText(String.format("Humidity: %.0f%%", humidity)); // Humidity doesn't need conversion
            windLabel.setText(String.format("Wind Speed: %.1f %s", displayWindSpeed, speedUnit));

            // Update forecast temperatures
            for (int i = 0, dayIndex = 0; i < forecastData.getJSONArray("list").length() && dayIndex < forecastDayBoxes.size(); i += 8, dayIndex++) {
                JSONObject day = forecastData.getJSONArray("list").getJSONObject(i);
                double dayTemp = day.getJSONObject("main").getDouble("temp");
                double displayDayTemp = convertTemperature(dayTemp, fetchedUnits, currentUnits);
                forecastDayBoxes.get(dayIndex).tempLabel.setText(String.format("%.1f%s", displayDayTemp, tempUnit));
            }

            // Add a fade animation to the weather labels for a smooth transition
            FadeTransition fade = new FadeTransition(Duration.seconds(1), weatherLabel);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

        } catch (Exception e) {
            showError("Error updating temperature display: " + e.getMessage());
            lastSearchSuccessful = false; // Mark the search as unsuccessful
        }
    }

    /**
     * Fetches JSON data from the specified URL (used for API requests).
     * Handles various HTTP error codes with specific error messages.
     *
     * @param urlString The URL to fetch data from
     * @return A JSONObject containing the API response
     * @throws Exception If an error occurs during the HTTP request
     */
    private JSONObject getJsonFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // Read the error message from the API response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            throw new Exception("Server returned HTTP response code: " + responseCode + " for URL: " + urlString + " - " + errorResponse.toString());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return new JSONObject(response.toString());
    }

    /**
     * Displays an error message on the GUI and resets the UI to its default state.
     *
     * @param message The error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        weatherLabel.setText("Temperature: --");
        humidityLabel.setText("Humidity: --");
        windLabel.setText("Wind Speed: --");
        conditionLabel.setText("Conditions: --");
        weatherIcon.setImage(null);
        forecastBox.getChildren().clear();
        forecastDayBoxes.clear();
        currentCondition = "clear"; // Reset background to default
        setDynamicBackground((VBox) weatherLabel.getParent().getParent());
    }

    /**
     * The main entry point for the JavaFX application.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}