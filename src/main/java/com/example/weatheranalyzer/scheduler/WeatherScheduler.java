package com.example.weatheranalyzer.scheduler;

import com.example.weatheranalyzer.entity.WeatherMeasurement;
import com.example.weatheranalyzer.exception.WeatherApiException;
import com.example.weatheranalyzer.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "weather.scheduling",
        name = "enabled",
        havingValue = "true"
)
public class WeatherScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeatherScheduler.class);

    private final WeatherService weatherService;

    public WeatherScheduler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Scheduled(
            fixedDelayString = "${weather.scheduling.fixed-delay}",
            initialDelayString = "${weather.scheduling.initial-delay}"
    )
    public void collectWeather() {
        try {
            WeatherMeasurement measurement = weatherService.collectAndSaveWeather();
            log.info("Weather saved: id={}, location={}, temperatureC={}",
                    measurement.getId(), measurement.getLocation(), measurement.getTemperatureC());
        } catch (WeatherApiException | DataAccessException exception) {
            log.error("Failed to collect weather: {}", exception.getMessage());
        }
    }
}
