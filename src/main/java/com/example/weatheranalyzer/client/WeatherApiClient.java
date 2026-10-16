package com.example.weatheranalyzer.client;

import com.example.weatheranalyzer.config.WeatherApiProperties;
import com.example.weatheranalyzer.dto.OpenWeatherResponse;
import com.example.weatheranalyzer.exception.WeatherApiException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WeatherApiClient {

    private final RestClient restClient;
    private final WeatherApiProperties properties;

    public WeatherApiClient(
            RestClient restClient,
            WeatherApiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OpenWeatherResponse fetchCurrentWeather() {
        String apiKey = properties.apiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new WeatherApiException(
                    "WEATHER_API_KEY is not configured"
            );
        }

        try {
            OpenWeatherResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/2.5/weather")
                            .queryParam("lat", properties.latitude())
                            .queryParam("lon", properties.longitude())
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", "ru")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OpenWeatherResponse.class);

            if (response == null) {
                throw new WeatherApiException(
                        "OpenWeather returned an empty response"
                );
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new WeatherApiException(
                    "OpenWeather returned HTTP "
                            + exception.getStatusCode().value()
            );
        } catch (RestClientException exception) {
            throw new WeatherApiException(
                    "Failed to retrieve weather from OpenWeather"
            );
        }
    }
}
