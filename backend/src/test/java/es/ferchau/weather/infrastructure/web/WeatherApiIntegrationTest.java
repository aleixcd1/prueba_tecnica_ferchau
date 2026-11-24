package es.ferchau.weather.infrastructure.web;

import es.ferchau.weather.domain.model.DailyForecast;
import es.ferchau.weather.domain.model.Municipality;
import es.ferchau.weather.domain.model.PrecipitationProbability;
import es.ferchau.weather.domain.port.out.MunicipalityCatalog;
import es.ferchau.weather.domain.port.out.WeatherForecastProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WeatherApiIntegrationTest {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private MunicipalityCatalog municipalityCatalog;

    @MockitoBean
    private WeatherForecastProvider weatherForecastProvider;

    @BeforeEach
    void configureMocks() {
        when(municipalityCatalog.findAll()).thenReturn(List.of(
                new Municipality("28079", "Madrid", "Madrid")));
        when(weatherForecastProvider.findForecast(
                org.mockito.ArgumentMatchers.eq("28079"),
                org.mockito.ArgumentMatchers.any(java.time.LocalDate.class)))
                .thenReturn(new DailyForecast(
                        new BigDecimal("20.0"),
                        List.of(
                                new PrecipitationProbability(5, "00-06"),
                                new PrecipitationProbability(10, "06-12"),
                                new PrecipitationProbability(10, "12-18"),
                                new PrecipitationProbability(5, "18-24"))));
    }

    @Test
    void searchesMunicipalitiesThroughTheHttpApi() throws Exception {
        var response = get("/api/v1/municipalities?query=madri");
        JsonNode municipality = objectMapper.readTree(response.body()).get(0);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(municipality.path("id").asText()).isEqualTo("28079");
        assertThat(municipality.path("nombre").asText()).isEqualTo("Madrid");
    }

    @Test
    void returnsConvertedForecastAndHourlyPrecipitationPeriods() throws Exception {
        var response = get("/api/v1/forecast/28079?unit=G_FAH");
        JsonNode forecast = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(forecast.path("mediaTemperatura").decimalValue()).isEqualTo(new BigDecimal("68.0"));
        assertThat(forecast.path("unidadTemperatura").asText()).isEqualTo("G_FAH");
        JsonNode periods = forecast.path("probPrecipitacion");
        assertThat(periods.size()).isEqualTo(4);
        assertThat(periods.get(0).path("periodo").asText()).isEqualTo("00-06");
        assertThat(periods.get(3).path("periodo").asText()).isEqualTo("18-24");
    }

    @Test
    void rejectsUnsupportedTemperatureUnits() throws Exception {
        var response = get("/api/v1/forecast/28079?unit=KELVIN");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void exposesHealthEndpointForContainerOrchestration() throws Exception {
        var response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).path("status").asText()).isEqualTo("UP");
    }

    @Test
    void servesTheOpenApiSpecification() throws Exception {
        var response = get("/v3/api-docs");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).path("info").path("title").asText())
                .isEqualTo("Previsión meteorológica");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
