package com.example.weatheranalyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AverageTemperatureResponse(
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "dd-MM-uuuu"
        )
        LocalDate from,

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "dd-MM-uuuu"
        )
        LocalDate to,

        @JsonProperty("average_temp")
        BigDecimal averageTemp,

        @JsonProperty("daily_averages")
        List<DailyAverageTemperatureResponse> dailyAverages
) {
}
