package com.example.weatheranalyzer.controller;

import com.example.weatheranalyzer.dto.AverageTemperatureRequest;
import com.example.weatheranalyzer.dto.AverageTemperatureResponse;
import com.example.weatheranalyzer.dto.CurrentWeatherResponse;
import com.example.weatheranalyzer.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    public CurrentWeatherResponse getCurrentWeather() {
        return weatherService.getCurrentWeather();
    }

    @PostMapping("/average-temperature")
    public AverageTemperatureResponse getAverageTemperature(
            @RequestBody AverageTemperatureRequest request
    ) {
        return weatherService.getAverageTemperature(request);
    }
}
