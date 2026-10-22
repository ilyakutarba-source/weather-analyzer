package com.example.weatheranalyzer.service;

import com.example.weatheranalyzer.client.WeatherApiClient;
import com.example.weatheranalyzer.dto.OpenWeatherResponse;
import com.example.weatheranalyzer.entity.WeatherMeasurement;
import com.example.weatheranalyzer.exception.WeatherApiException;
import com.example.weatheranalyzer.repository.WeatherMeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final Instant MEASURED_AT =
            Instant.parse("2026-10-22T10:00:00Z");

    @Mock
    private WeatherApiClient weatherApiClient;

    @Mock
    private WeatherMeasurementRepository repository;

    private WeatherService service;

    @BeforeEach
    void setUp() {
        service = new WeatherService(weatherApiClient, repository);
    }

    @Test
    void shouldConvertAndSaveWeather() {
        OpenWeatherResponse response = createResponse(
                List.of(new OpenWeatherResponse.Condition("пасмурно")),
                79
        );

        when(weatherApiClient.fetchCurrentWeather())
                .thenReturn(response);

        when(repository.save(any(WeatherMeasurement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WeatherMeasurement result = service.collectAndSaveWeather();

        ArgumentCaptor<WeatherMeasurement> captor =
                ArgumentCaptor.forClass(WeatherMeasurement.class);

        verify(repository).save(captor.capture());

        WeatherMeasurement measurement = captor.getValue();

        assertSame(measurement, result);
        assertEquals(
                new BigDecimal("6.7"),
                measurement.getTemperatureC()
        );
        assertEquals(
                new BigDecimal("6984.00"),
                measurement.getWindSpeedMetersPerHour()
        );
        assertEquals(
                new BigDecimal("1023"),
                measurement.getPressureHpa()
        );
        assertEquals(
                Integer.valueOf(79),
                measurement.getHumidity()
        );
        assertEquals("пасмурно", measurement.getWeatherCondition());
        assertEquals("Минск", measurement.getLocation());
        assertEquals(MEASURED_AT, measurement.getMeasuredAt());
    }

    @Test
    void shouldNotSaveWhenWeatherApiFails() {
        WeatherApiException expected =
                new WeatherApiException("OpenWeather returned HTTP 503");

        when(weatherApiClient.fetchCurrentWeather())
                .thenThrow(expected);

        WeatherApiException actual = assertThrows(
                WeatherApiException.class,
                () -> service.collectAndSaveWeather()
        );

        assertSame(expected, actual);
        verifyNoInteractions(repository);
    }

    @Test
    void shouldNotSaveWhenWeatherConditionsAreMissing() {
        OpenWeatherResponse response = createResponse(
                List.of(),
                79
        );

        when(weatherApiClient.fetchCurrentWeather())
                .thenReturn(response);

        assertThrows(
                WeatherApiException.class,
                () -> service.collectAndSaveWeather()
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldNotSaveWhenHumidityIsMissing() {
        OpenWeatherResponse response = createResponse(
                List.of(new OpenWeatherResponse.Condition("пасмурно")),
                null
        );

        when(weatherApiClient.fetchCurrentWeather())
                .thenReturn(response);

        assertThrows(
                WeatherApiException.class,
                () -> service.collectAndSaveWeather()
        );

        verifyNoInteractions(repository);
    }

    private OpenWeatherResponse createResponse(
            List<OpenWeatherResponse.Condition> conditions,
            Integer humidity
    ) {
        return new OpenWeatherResponse(
                new OpenWeatherResponse.Main(
                        new BigDecimal("6.7"),
                        new BigDecimal("1023"),
                        humidity
                ),
                new OpenWeatherResponse.Wind(
                        new BigDecimal("1.94")
                ),
                conditions,
                "Минск",
                MEASURED_AT.getEpochSecond()
        );
    }
}