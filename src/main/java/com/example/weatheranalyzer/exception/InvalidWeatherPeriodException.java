package com.example.weatheranalyzer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidWeatherPeriodException extends RuntimeException {

    public InvalidWeatherPeriodException(String message) {
        super(message);
    }
}
