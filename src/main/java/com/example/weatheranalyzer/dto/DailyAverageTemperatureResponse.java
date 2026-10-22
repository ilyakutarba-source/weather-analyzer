package com.example.weatheranalyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAverageTemperatureResponse(
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "dd-MM-uuuu"
        )
        LocalDate date,

        @JsonProperty("average_temp")
        BigDecimal averageTemp
) {
}
