package com.example.weatheranalyzer.repository;

import com.example.weatheranalyzer.entity.WeatherMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WeatherMeasurementRepository
        extends JpaRepository<WeatherMeasurement, Long> {

    Optional<WeatherMeasurement> findFirstByOrderByMeasuredAtDescIdDesc();

    @Query("""
            SELECT measurement
            FROM WeatherMeasurement measurement
            WHERE measurement.measuredAt >= :fromInclusive
              AND measurement.measuredAt < :toExclusive
            ORDER BY measurement.measuredAt ASC, measurement.id ASC
            """)
    List<WeatherMeasurement> findAllInPeriod(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
