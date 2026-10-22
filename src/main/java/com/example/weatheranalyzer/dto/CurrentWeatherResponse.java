package com.example.weatheranalyzer.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CurrentWeatherResponse(
        BigDecimal temperatureC,
        BigDecimal windSpeedMetersPerHour,
        BigDecimal pressureHpa,
        Integer humidity,
        String weatherCondition,
        String location,
        Instant measuredAt
) {
}