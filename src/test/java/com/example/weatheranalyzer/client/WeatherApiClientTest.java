package com.example.weatheranalyzer.client;

import com.example.weatheranalyzer.config.WeatherApiProperties;
import com.example.weatheranalyzer.dto.OpenWeatherResponse;
import com.example.weatheranalyzer.exception.WeatherApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherApiClientTest {

    private static final String BASE_URL =
            "https://api.openweathermap.org";

    private MockRestServiceServer server;
    private RestClient restClient;
    private WeatherApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL);

        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        client = new WeatherApiClient(
                restClient,
                createProperties("test-key")
        );
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void shouldRequestWeatherAndDeserializeResponse() {
        server.expect(requestTo(
                        startsWith(BASE_URL + "/data/2.5/weather?")
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("lat", "53.9"))
                .andExpect(queryParam("lon", "27.5667"))
                .andExpect(queryParam("appid", "test-key"))
                .andExpect(queryParam("units", "metric"))
                .andExpect(queryParam("lang", "ru"))
                .andExpect(header(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                ))
                .andRespond(withSuccess("""
                        {
                          "name": "Минск",
                          "dt": 1792188194,
                          "visibility": 10000,
                          "main": {
                            "temp": 6.7,
                            "pressure": 1023,
                            "humidity": 79,
                            "feels_like": 5.41
                          },
                          "wind": {
                            "speed": 1.94,
                            "deg": 21
                          },
                          "weather": [
                            {
                              "id": 804,
                              "description": "пасмурно",
                              "icon": "04n"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        OpenWeatherResponse response = client.fetchCurrentWeather();

        assertEquals("Минск", response.locationName());
        assertEquals(
                Long.valueOf(1792188194L),
                response.measuredAtEpochSeconds()
        );
        assertEquals(
                new BigDecimal("6.7"),
                response.main().temperatureC()
        );
        assertEquals(
                new BigDecimal("1023"),
                response.main().pressureHpa()
        );
        assertEquals(
                Integer.valueOf(79),
                response.main().humidity()
        );
        assertEquals(
                new BigDecimal("1.94"),
                response.wind().speedMetersPerSecond()
        );
        assertEquals(1, response.weather().size());
        assertEquals(
                "пасмурно",
                response.weather().get(0).description()
        );
    }

    @Test
    void shouldThrowWeatherApiExceptionWhenProviderReturnsUnauthorized() {
        server.expect(requestTo(
                        startsWith(BASE_URL + "/data/2.5/weather?")
                ))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "cod": 401,
                                  "message": "Invalid API key"
                                }
                                """));

        WeatherApiException exception = assertThrows(
                WeatherApiException.class,
                () -> client.fetchCurrentWeather()
        );

        assertEquals(
                "OpenWeather returned HTTP 401",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankApiKeyWithoutSendingRequest() {
        client = new WeatherApiClient(
                restClient,
                createProperties("   ")
        );

        WeatherApiException exception = assertThrows(
                WeatherApiException.class,
                () -> client.fetchCurrentWeather()
        );

        assertEquals(
                "WEATHER_API_KEY is not configured",
                exception.getMessage()
        );
    }

    private WeatherApiProperties createProperties(String apiKey) {
        return new WeatherApiProperties(
                BASE_URL,
                apiKey,
                53.9,
                27.5667,
                Duration.ofSeconds(5),
                Duration.ofSeconds(10)
        );
    }
}
