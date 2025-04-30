 Weather Odyssey

## Overview
Weather Odyssey is a desktop application built with Java and JavaFX that fetches and displays real-time weather data using the OpenWeatherMap API. It offers an intuitive interface with features like current weather details, a 5-day forecast, unit conversion, and a dynamic background that adapts to weather conditions. This project highlights skills in Java programming, API integration, and UI design.

## Features
- **City Search**: Retrieve current weather and a 5-day forecast by entering a city name.
- **Unit Conversion**: Toggle between Celsius and Fahrenheit with real-time updates.
- **Search History**: View past successful searches with timestamps.
- **Dynamic Background**: The UI background changes based on weather (e.g., yellow for clear skies, blue for rain).
- **Error Handling**: User-friendly messages for invalid inputs or API issues.
- **Loading Indicator**: A spinner ensures responsiveness during data fetching.

## Technologies Used
- **Java 11**: Core programming language.
- **JavaFX 17**: Framework for the graphical user interface.
- **OpenWeatherMap API**: Source of weather data.
- **JSONObject**: For parsing API responses.

## Installation
1. **Prerequisites**: Ensure Java 11+ and JavaFX SDK are installed.
2. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/Weather-Odyssey.git
   ```
3. Navigate to the project directory:
   ```bash
   cd Weather-Odyssey
   ```
4. Compile the code (replace `path/to/javafx-sdk` with your JavaFX SDK path):
   ```bash
   javac -cp .:path/to/javafx-sdk/lib/* src/WeatherApp.java
   ```
5. Run the application:
   ```bash
   java -cp .:path/to/javafx-sdk/lib/* WeatherApp
   ```
**Note**: You’ll need an OpenWeatherMap API key. Replace the hardcoded key in `WeatherApp.java` with your own or use an environment variable.

## Usage
1. Launch the application.
2. Enter a city name (e.g., "London") in the text field.
3. Select Celsius or Fahrenheit from the dropdown.
4. Click "Search" to fetch weather data.
5. Explore current conditions, the 5-day forecast, and search history.
6. Use "Refresh" to update the last searched city.
7. Switch units to adjust the display dynamically.

## Project Structure
```
Weather-Odyssey/
├── src/
│   └── WeatherApp.java  # Main application code
├── README.md           # Project documentation
└── .gitignore          # Excludes IDE files and compiled classes
```

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Future Enhancements
- Add support for multiple cities.
- Include additional weather metrics (e.g., UV index).
- Develop a mobile version using JavaFXPorts.