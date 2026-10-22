/**
 * @author Ilya
 * @date 22.10.2026
 */
package com.example.weatheranalyzer.service;

import com.example.weatheranalyzer.client.WeatherApiClient;
import com.example.weatheranalyzer.dto.AverageTemperatureRequest;
import com.example.weatheranalyzer.dto.AverageTemperatureResponse;
import com.example.weatheranalyzer.dto.CurrentWeatherResponse;
import com.example.weatheranalyzer.dto.DailyAverageTemperatureResponse;
import com.example.weatheranalyzer.dto.OpenWeatherResponse;
import com.example.weatheranalyzer.entity.WeatherMeasurement;
import com.example.weatheranalyzer.exception.InvalidWeatherPeriodException;
import com.example.weatheranalyzer.exception.WeatherApiException;
import com.example.weatheranalyzer.exception.WeatherNotFoundException;
import com.example.weatheranalyzer.repository.WeatherMeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class WeatherService {

    private static final BigDecimal SECONDS_PER_HOUR =
            BigDecimal.valueOf(3600);

    private static final ZoneId MINSK_ZONE =
            ZoneId.of("Europe/Minsk");

    private final WeatherApiClient weatherApiClient;
    private final WeatherMeasurementRepository repository;

    public WeatherService(
            WeatherApiClient weatherApiClient,
            WeatherMeasurementRepository repository
    ) {
        this.weatherApiClient = weatherApiClient;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CurrentWeatherResponse getCurrentWeather() {
        WeatherMeasurement measurement = repository
                .findFirstByOrderByMeasuredAtDescIdDesc()
                .orElseThrow(() -> new WeatherNotFoundException(
                        "No weather measurements have been saved yet"
                ));

        return new CurrentWeatherResponse(
                measurement.getTemperatureC(),
                measurement.getWindSpeedMetersPerHour(),
                measurement.getPressureHpa(),
                measurement.getHumidity(),
                measurement.getWeatherCondition(),
                measurement.getLocation(),
                measurement.getMeasuredAt()
        );
    }

    @Transactional(readOnly = true)
    public AverageTemperatureResponse getAverageTemperature(
            AverageTemperatureRequest request
    ) {
        validatePeriod(request);

        Instant fromInclusive = request.from()
                .atStartOfDay(MINSK_ZONE)
                .toInstant();

        Instant toExclusive = request.to()
                .plusDays(1)
                .atStartOfDay(MINSK_ZONE)
                .toInstant();

        List<WeatherMeasurement> measurements =
                repository.findAllInPeriod(fromInclusive, toExclusive);

        if (measurements.isEmpty()) {
            throw new WeatherNotFoundException(
                    "No weather measurements found for the requested period"
            );
        }

        Map<LocalDate, List<WeatherMeasurement>> measurementsByDate =
                new TreeMap<>();

        for (WeatherMeasurement measurement : measurements) {
            LocalDate date = measurement.getMeasuredAt()
                    .atZone(MINSK_ZONE)
                    .toLocalDate();

            measurementsByDate
                    .computeIfAbsent(date, key -> new ArrayList<>())
                    .add(measurement);
        }

        List<DailyAverageTemperatureResponse> dailyAverages =
                new ArrayList<>();

        for (Map.Entry<LocalDate, List<WeatherMeasurement>> entry
                : measurementsByDate.entrySet()) {
            dailyAverages.add(new DailyAverageTemperatureResponse(
                    entry.getKey(),
                    calculateAverageTemperature(entry.getValue())
            ));
        }

        return new AverageTemperatureResponse(
                request.from(),
                request.to(),
                calculateAverageTemperature(measurements),
                dailyAverages
        );
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

    private void validatePeriod(AverageTemperatureRequest request) {
        if (request == null
                || request.from() == null
                || request.to() == null) {
            throw new InvalidWeatherPeriodException(
                    "Both from and to dates are required"
            );
        }

        if (request.from().isAfter(request.to())) {
            throw new InvalidWeatherPeriodException(
                    "The from date must not be after the to date"
            );
        }
    }

    private BigDecimal calculateAverageTemperature(
            List<WeatherMeasurement> measurements
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (WeatherMeasurement measurement : measurements) {
            total = total.add(measurement.getTemperatureC());
        }

        return total.divide(
                BigDecimal.valueOf(measurements.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    private void validateResponse(OpenWeatherResponse response) {
        if (response == null
                || response.main() == null
                || response.wind() == null
                || response.weather() == null
                || response.weather().isEmpty()
                || response.weather().get(0) == null) {
            throw new WeatherApiException(
                    "OpenWeather returned incomplete weather data"
            );
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
