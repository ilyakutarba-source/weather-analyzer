/**
 * @author Ilya
 * @date 17.10.2026
 */
package com.example.weatheranalyzer.repository;

import com.example.weatheranalyzer.entity.WeatherMeasurement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class WeatherMeasurementRepositoryTest {

    private final WeatherMeasurementRepository repository;
    private final TestEntityManager entityManager;

    @Autowired
    WeatherMeasurementRepositoryTest(
            WeatherMeasurementRepository repository,
            TestEntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void shouldSaveAndReadMeasurement() {
        WeatherMeasurement saved =
                saveMeasurement("2026-10-17T09:00:00Z");

        assertNotNull(saved.getId());

        entityManager.clear();

        WeatherMeasurement actual = repository.findById(saved.getId())
                .orElseThrow();

        assertEquals(saved.getId(), actual.getId());
        assertEquals(new BigDecimal("12.35"), actual.getTemperatureC());
        assertEquals(
                new BigDecimal("12000.00"),
                actual.getWindSpeedMetersPerHour()
        );
        assertEquals(new BigDecimal("1013.25"), actual.getPressureHpa());
        assertEquals(Integer.valueOf(60), actual.getHumidity());
        assertEquals("Облачно", actual.getWeatherCondition());
        assertEquals("Минск", actual.getLocation());
        assertEquals(
                Instant.parse("2026-10-17T09:00:00Z"),
                actual.getMeasuredAt()
        );
    }

    @Test
    void shouldFindMostRecentMeasurement() {
        saveMeasurement("2026-10-17T09:00:00Z");

        WeatherMeasurement expected =
                saveMeasurement("2026-10-17T10:00:00Z");

        saveMeasurement("2026-10-17T08:00:00Z");

        entityManager.clear();

        WeatherMeasurement actual = repository
                .findFirstByOrderByMeasuredAtDescIdDesc()
                .orElseThrow();

        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    void shouldUseIdWhenMeasurementTimesAreEqual() {
        saveMeasurement("2026-10-17T10:00:00Z");

        WeatherMeasurement expected =
                saveMeasurement("2026-10-17T10:00:00Z");

        entityManager.clear();

        WeatherMeasurement actual = repository
                .findFirstByOrderByMeasuredAtDescIdDesc()
                .orElseThrow();

        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    void shouldReturnEmptyWhenNoMeasurementsExist() {
        assertTrue(
                repository.findFirstByOrderByMeasuredAtDescIdDesc().isEmpty()
        );
    }

    private WeatherMeasurement saveMeasurement(String measuredAt) {
        WeatherMeasurement measurement = new WeatherMeasurement(
                new BigDecimal("12.35"),
                new BigDecimal("12000.00"),
                new BigDecimal("1013.25"),
                60,
                "Облачно",
                "Минск",
                Instant.parse(measuredAt)
        );

        return repository.saveAndFlush(measurement);
    }
}