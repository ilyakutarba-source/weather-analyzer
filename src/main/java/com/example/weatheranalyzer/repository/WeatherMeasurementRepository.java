package com.example.weatheranalyzer.repository;

import com.example.weatheranalyzer.entity.WeatherMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherMeasurementRepository extends JpaRepository<WeatherMeasurement, Long> {
    Optional<WeatherMeasurement> findFirstByOrderByMeasuredAtDescIdDesc();
}
