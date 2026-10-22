package com.example.weatheranalyzer.service;

import com.example.weatheranalyzer.client.WeatherApiClient;
import com.example.weatheranalyzer.dto.AverageTemperatureRequest;
import com.example.weatheranalyzer.dto.AverageTemperatureResponse;
import com.example.weatheranalyzer.dto.DailyAverageTemperatureResponse;
import com.example.weatheranalyzer.dto.OpenWeatherResponse;
import com.example.weatheranalyzer.entity.WeatherMeasurement;
import com.example.weatheranalyzer.exception.InvalidWeatherPeriodException;
import com.example.weatheranalyzer.exception.WeatherApiException;
import com.example.weatheranalyzer.exception.WeatherNotFoundException;
import com.example.weatheranalyzer.repository.WeatherMeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Test
    void shouldCalculatePeriodAndDailyAveragesUsingMinskDates() {
        AverageTemperatureRequest request = new AverageTemperatureRequest(
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 10, 24)
        );
        Instant fromInclusive = Instant.parse("2026-10-21T21:00:00Z");
        Instant toExclusive = Instant.parse("2026-10-24T21:00:00Z");

        when(repository.findAllInPeriod(fromInclusive, toExclusive))
                .thenReturn(List.of(
                        createMeasurement("16.01", "2026-10-23T21:00:00Z"),
                        createMeasurement("10.00", "2026-10-22T20:59:59Z"),
                        createMeasurement("10.00", "2026-10-21T21:00:00Z")
                ));

        AverageTemperatureResponse result = service.getAverageTemperature(request);

        assertEquals(request.from(), result.from());
        assertEquals(request.to(), result.to());
        assertEquals(new BigDecimal("12.00"), result.averageTemp());
        assertEquals(List.of(
                new DailyAverageTemperatureResponse(
                        LocalDate.of(2026, 10, 22), new BigDecimal("10.00")),
                new DailyAverageTemperatureResponse(
                        LocalDate.of(2026, 10, 24), new BigDecimal("16.01"))
        ), result.dailyAverages());
        verify(repository).findAllInPeriod(fromInclusive, toExclusive);
        verifyNoInteractions(weatherApiClient);
    }

    @ParameterizedTest
    @CsvSource({
            "1.00, 1.01, 1.01",
            "-1.00, -1.01, -1.01"
    })
    void shouldRoundAveragesForSingleDay(
            String firstTemperature,
            String secondTemperature,
            String expectedAverage
    ) {
        LocalDate date = LocalDate.of(2026, 10, 22);
        AverageTemperatureRequest request = new AverageTemperatureRequest(date, date);

        when(repository.findAllInPeriod(
                Instant.parse("2026-10-21T21:00:00Z"),
                Instant.parse("2026-10-22T21:00:00Z")
        )).thenReturn(List.of(
                createMeasurement(firstTemperature, "2026-10-22T09:00:00Z"),
                createMeasurement(secondTemperature, "2026-10-22T10:00:00Z")
        ));

        AverageTemperatureResponse result = service.getAverageTemperature(request);

        assertEquals(new BigDecimal(expectedAverage), result.averageTemp());
        assertEquals(List.of(new DailyAverageTemperatureResponse(
                date, new BigDecimal(expectedAverage)
        )), result.dailyAverages());
        verifyNoInteractions(weatherApiClient);
    }

    @Test
    void shouldRejectMissingPeriod() {
        assertThrows(InvalidWeatherPeriodException.class,
                () -> service.getAverageTemperature(null));

        verifyNoInteractions(repository, weatherApiClient);
    }

    @Test
    void shouldRejectMissingPeriodDates() {
        LocalDate date = LocalDate.of(2026, 10, 22);

        for (AverageTemperatureRequest request : List.of(
                new AverageTemperatureRequest(null, date),
                new AverageTemperatureRequest(date, null)
        )) {
            assertThrows(InvalidWeatherPeriodException.class,
                    () -> service.getAverageTemperature(request));
        }

        verifyNoInteractions(repository, weatherApiClient);
    }

    @Test
    void shouldRejectReversedPeriod() {
        AverageTemperatureRequest request = new AverageTemperatureRequest(
                LocalDate.of(2026, 10, 23),
                LocalDate.of(2026, 10, 22)
        );

        assertThrows(InvalidWeatherPeriodException.class,
                () -> service.getAverageTemperature(request));

        verifyNoInteractions(repository, weatherApiClient);
    }

    @Test
    void shouldRejectPeriodWithoutMeasurements() {
        LocalDate date = LocalDate.of(2026, 10, 22);
        AverageTemperatureRequest request = new AverageTemperatureRequest(date, date);

        when(repository.findAllInPeriod(
                Instant.parse("2026-10-21T21:00:00Z"),
                Instant.parse("2026-10-22T21:00:00Z")
        )).thenReturn(List.of());

        assertThrows(WeatherNotFoundException.class,
                () -> service.getAverageTemperature(request));

        verifyNoInteractions(weatherApiClient);
    }

    private WeatherMeasurement createMeasurement(String temperature, String measuredAt) {
        return new WeatherMeasurement(
                new BigDecimal(temperature),
                BigDecimal.ONE,
                new BigDecimal("1023"),
                79,
                "пасмурно",
                "Минск",
                Instant.parse(measuredAt)
        );
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
