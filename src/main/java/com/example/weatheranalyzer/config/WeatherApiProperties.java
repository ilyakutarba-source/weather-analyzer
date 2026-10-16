package com.example.weatheranalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "weather.api")
public record WeatherApiProperties(
        String baseUrl,
        String apiKey,
        double latitude,
        double longitude,
        Duration connectTimeout,
        Duration readTimeout
) {
}