/**
 * @author Ilya
 * @date 22.10.2026
 */
package com.example.weatheranalyzer.service;


import com.example.weatheranalyzer.client.WeatherApiClient;
import com.example.weatheranalyzer.dto.OpenWeatherResponse;
import com.example.weatheranalyzer.entity.WeatherMeasurement;
import com.example.weatheranalyzer.exception.WeatherApiException;
import com.example.weatheranalyzer.repository.WeatherMeasurementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class WeatherService {

    private static final BigDecimal SECONDS_PER_HOUR =
            BigDecimal.valueOf(3600);

    private final WeatherApiClient weatherApiClient;
    private final WeatherMeasurementRepository repository;

    public WeatherService(
            WeatherApiClient weatherApiClient,
            WeatherMeasurementRepository repository
    ) {
        this.weatherApiClient = weatherApiClient;
        this.repository = repository;
    }

    public WeatherMeasurement collectAndSaveWeather() {
        OpenWeatherResponse response =
                weatherApiClient.fetchCurrentWeather();

        validateResponse(response);

        BigDecimal windSpeedMetersPerHour = response.wind()
                .speedMetersPerSecond()
                .multiply(SECONDS_PER_HOUR);

        WeatherMeasurement measurement = new WeatherMeasurement(
                response.main().temperatureC(),
                windSpeedMetersPerHour,
                response.main().pressureHpa(),
                response.main().humidity(),
                response.weather().get(0).description(),
                response.locationName(),
                Instant.ofEpochSecond(response.measuredAtEpochSeconds())
        );

        return repository.save(measurement);
    }

    private void validateResponse(OpenWeatherResponse response) {
        if (response == null
                || response.main() == null
                || response.wind() == null
                || response.weather() == null
                || response.weather().isEmpty()
                || response.weather().get(0) == null) {
            throw new WeatherApiException(
                    "OpenWeather returned incomplete weather data");
        }

        OpenWeatherResponse.Main main = response.main();
        OpenWeatherResponse.Wind wind = response.wind();
        String condition = response.weather().get(0).description();

        if (main.temperatureC() == null
                || main.pressureHpa() == null
                || main.humidity() == null
                || wind.speedMetersPerSecond() == null
                || response.measuredAtEpochSeconds() == null
                || response.locationName() == null
                || response.locationName().isBlank()
                || condition == null
                || condition.isBlank()) {
            throw new WeatherApiException(
                    "OpenWeather response is missing required weather values"
            );
        }
    }
}