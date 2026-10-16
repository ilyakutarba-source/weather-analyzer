/**
 * @author Ilya
 * @date 16.10.2026
 */
package com.example.weatheranalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "weather_measurements")
public class WeatherMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "wind_speed_meters_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal windSpeedMetersPerHour;

    @Column(name = "pressure_hpa", nullable = false, precision = 7, scale = 2)
    private BigDecimal pressureHpa;

    @Column(name = "humidity", nullable = false)
    private Integer humidity;

    @Column(name = "weather_condition", nullable = false, length = 255)
    private String weatherCondition;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    protected WeatherMeasurement() {
    }

    public WeatherMeasurement(
            BigDecimal temperatureC,
            BigDecimal windSpeedMetersPerHour,
            BigDecimal pressureHpa,
            Integer humidity,
            String weatherCondition,
            String location,
            Instant measuredAt
    ) {
        this.temperatureC = temperatureC;
        this.windSpeedMetersPerHour = windSpeedMetersPerHour;
        this.pressureHpa = pressureHpa;
        this.humidity = humidity;
        this.weatherCondition = weatherCondition;
        this.location = location;
        this.measuredAt = measuredAt;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getTemperatureC() {
        return temperatureC;
    }

    public BigDecimal getWindSpeedMetersPerHour() {
        return windSpeedMetersPerHour;
    }

    public BigDecimal getPressureHpa() {
        return pressureHpa;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public String getLocation() {
        return location;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }
}
