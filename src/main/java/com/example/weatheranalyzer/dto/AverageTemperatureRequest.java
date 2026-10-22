package com.example.weatheranalyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;

import java.time.LocalDate;

public record AverageTemperatureRequest(
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "dd-MM-uuuu",
                lenient = OptBoolean.FALSE
        )
        LocalDate from,

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "dd-MM-uuuu",
                lenient = OptBoolean.FALSE
        )
        LocalDate to
) {
}