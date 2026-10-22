package com.example.weatheranalyzer.exception;

public class WeatherNotFoundException extends RuntimeException {

    public WeatherNotFoundException(String message) {
        super(message);
    }
}
