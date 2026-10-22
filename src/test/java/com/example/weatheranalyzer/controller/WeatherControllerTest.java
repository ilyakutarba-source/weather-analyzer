package com.example.weatheranalyzer.controller;

import com.example.weatheranalyzer.dto.AverageTemperatureRequest;
import com.example.weatheranalyzer.dto.AverageTemperatureResponse;
import com.example.weatheranalyzer.dto.CurrentWeatherResponse;
import com.example.weatheranalyzer.dto.DailyAverageTemperatureResponse;
import com.example.weatheranalyzer.exception.InvalidWeatherPeriodException;
import com.example.weatheranalyzer.exception.WeatherNotFoundException;
import com.example.weatheranalyzer.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
@ActiveProfiles("test")
class WeatherControllerTest {

    private static final String CURRENT_ENDPOINT =
            "/api/weather/current";

    private static final String AVERAGE_ENDPOINT =
            "/api/weather/average-temperature";

    private final MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @Autowired
    WeatherControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldReturnCurrentWeather() throws Exception {
        CurrentWeatherResponse response = new CurrentWeatherResponse(
                new BigDecimal("12.02"),
                new BigDecimal("18828.00"),
                new BigDecimal("1022.00"),
                84,
                "облачно с прояснениями",
                "Минск",
                Instant.parse("2026-10-22T11:27:00Z")
        );

        when(weatherService.getCurrentWeather()).thenReturn(response);

        mockMvc.perform(get(CURRENT_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.temperatureC").value(12.02))
                .andExpect(jsonPath("$.windSpeedMetersPerHour").value(18828))
                .andExpect(jsonPath("$.pressureHpa").value(1022))
                .andExpect(jsonPath("$.humidity").value(84))
                .andExpect(jsonPath("$.weatherCondition")
                        .value("облачно с прояснениями"))
                .andExpect(jsonPath("$.location").value("Минск"))
                .andExpect(jsonPath("$.measuredAt")
                        .value("2026-10-22T11:27:00Z"));
    }

    @Test
    void shouldReturnAverageTemperature() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 22);

        AverageTemperatureRequest request =
                new AverageTemperatureRequest(date, date);

        AverageTemperatureResponse response =
                new AverageTemperatureResponse(
                        date,
                        date,
                        new BigDecimal("12.02"),
                        List.of(new DailyAverageTemperatureResponse(
                                date,
                                new BigDecimal("12.02")
                        ))
                );

        when(weatherService.getAverageTemperature(request))
                .thenReturn(response);

        mockMvc.perform(post(AVERAGE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "from": "22-10-2026",
                                  "to": "22-10-2026"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.from").value("22-10-2026"))
                .andExpect(jsonPath("$.to").value("22-10-2026"))
                .andExpect(jsonPath("$.average_temp").value(12.02))
                .andExpect(jsonPath("$.daily_averages.length()").value(1))
                .andExpect(jsonPath("$.daily_averages[0].date")
                        .value("22-10-2026"))
                .andExpect(jsonPath("$.daily_averages[0].average_temp")
                        .value(12.02));

        verify(weatherService).getAverageTemperature(request);
    }

    @Test
    void shouldReturnProblemWhenWeatherIsMissing() throws Exception {
        String message = "No weather measurements have been saved yet";

        when(weatherService.getCurrentWeather())
                .thenThrow(new WeatherNotFoundException(message));

        mockMvc.perform(get(CURRENT_ENDPOINT))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(message))
                .andExpect(jsonPath("$.instance").value(CURRENT_ENDPOINT));
    }

    @Test
    void shouldReturnProblemWhenPeriodIsInvalid() throws Exception {
        AverageTemperatureRequest request =
                new AverageTemperatureRequest(
                        LocalDate.of(2026, 10, 23),
                        LocalDate.of(2026, 10, 22)
                );

        String message = "The from date must not be after the to date";

        when(weatherService.getAverageTemperature(request))
                .thenThrow(new InvalidWeatherPeriodException(message));

        mockMvc.perform(post(AVERAGE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "from": "23-10-2026",
                                  "to": "22-10-2026"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(message))
                .andExpect(jsonPath("$.instance").value(AVERAGE_ENDPOINT));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"from\":\"31-02-2026\",\"to\":\"22-10-2026\"}",
            "{invalid-json}"
    })
    void shouldRejectInvalidRequestBody(String body) throws Exception {
        mockMvc.perform(post(AVERAGE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Invalid request body. Provide from and to dates "
                                + "in dd-MM-yyyy format."
                ))
                .andExpect(jsonPath("$.instance").value(AVERAGE_ENDPOINT));

        verifyNoInteractions(weatherService);
    }

    @Test
    void shouldHideUnexpectedErrorDetails() throws Exception {
        when(weatherService.getCurrentWeather())
                .thenThrow(new IllegalStateException(
                        "Internal database details"
                ));

        mockMvc.perform(get(CURRENT_ENDPOINT))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail")
                        .value("An unexpected server error occurred"))
                .andExpect(jsonPath("$.instance").value(CURRENT_ENDPOINT))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(
                        not(containsString("Internal database details"))
                ));
    }
}
