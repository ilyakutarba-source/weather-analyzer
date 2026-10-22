package com.example.weatheranalyzer;

import com.example.weatheranalyzer.scheduler.WeatherScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class WeatherAnalyzerApplicationTests {

    private final ApplicationContext context;

    @Autowired
    WeatherAnalyzerApplicationTests(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldDisableWeatherSchedulerInTests() {
        assertTrue(context.getBeansOfType(WeatherScheduler.class).isEmpty());
    }

}
