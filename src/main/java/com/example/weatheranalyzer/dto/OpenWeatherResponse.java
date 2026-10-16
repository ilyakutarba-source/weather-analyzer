package com.example.weatheranalyzer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherResponse(
        Main main,
        Wind wind,
        List<Condition> weather,
        @JsonProperty("name") String locationName,
        @JsonProperty("dt") Long measuredAtEpochSeconds
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Main(
            @JsonProperty("temp") BigDecimal temperatureC,
            @JsonProperty("pressure") BigDecimal pressureHpa,
            Integer humidity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Wind(
            @JsonProperty("speed") BigDecimal speedMetersPerSecond
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Condition(
            String description
    ) {
    }
}