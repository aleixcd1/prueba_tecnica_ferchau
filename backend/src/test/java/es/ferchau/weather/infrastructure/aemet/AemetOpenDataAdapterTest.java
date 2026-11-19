package es.ferchau.weather.infrastructure.aemet;

import es.ferchau.weather.domain.model.Municipality;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AemetOpenDataAdapterTest {
    private static final String BASE_URL = "https://opendata.aemet.es/opendata";

    @Test
    void followsAemetDataUrlAndMapsTheMunicipalityCatalogue() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AemetOpenDataAdapter adapter = new AemetOpenDataAdapter(
                builder.baseUrl(BASE_URL).build(), "test-key", BASE_URL);
        server.expect(requestTo(BASE_URL + "/api/maestro/municipios?api_key=test-key"))
                .andRespond(withSuccess(
                        "{\"estado\":200,\"datos\":\"" + BASE_URL + "/data/municipalities\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/data/municipalities"))
                .andRespond(withSuccess(
                        "[{\"id\":\"id28079\",\"nombre\":\"Madrid\",\"provincia\":\"Madrid\"}]",
                        MediaType.APPLICATION_JSON));

        assertThat(adapter.findAll()).containsExactly(new Municipality("28079", "Madrid", "Madrid"));
        server.verify();
    }

    @Test
    void mapsTheRequestedDayAndAggregatesPrecipitationIntoSixHourPeriods() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AemetOpenDataAdapter adapter = new AemetOpenDataAdapter(
                builder.baseUrl(BASE_URL).build(), "test-key", BASE_URL);
        LocalDate forecastDate = LocalDate.of(2026, 11, 20);
        server.expect(requestTo(BASE_URL
                        + "/api/prediccion/especifica/municipio/diaria/28079?api_key=test-key"))
                .andRespond(withSuccess(
                        "{\"estado\":200,\"datos\":\"" + BASE_URL + "/data/forecast\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/data/forecast"))
                .andRespond(withSuccess("""
                        [{
                          "prediccion": {
                            "dia": [{
                              "fecha": "2026-11-20T00:00:00",
                              "temperatura": [{"value": 10}, {"value": 20}],
                              "probPrecipitacion": [
                                {"value": 20, "periodo": "00-12"},
                                {"value": 40, "periodo": "12-24"}
                              ]
                            }]
                          }
                        }]
                        """, MediaType.APPLICATION_JSON));

        var forecast = adapter.findForecast("28079", forecastDate);

        assertThat(forecast.meanTemperatureCelsius()).isEqualTo(new BigDecimal("15.00"));
        assertThat(forecast.precipitationProbabilities())
                .extracting("probability")
                .containsExactly(20, 20, 40, 40);
        assertThat(forecast.precipitationProbabilities())
                .extracting("period")
                .containsExactlyElementsOf(List.of("00-06", "06-12", "12-18", "18-24"));
        server.verify();
    }

    @Test
    void rejectsDataUrlsOnAnotherHost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AemetOpenDataAdapter adapter = new AemetOpenDataAdapter(
                builder.baseUrl(BASE_URL).build(), "test-key", BASE_URL);
        server.expect(requestTo(BASE_URL + "/api/maestro/municipios?api_key=test-key"))
                .andRespond(withSuccess(
                        "{\"estado\":200,\"datos\":\"https://example.org/untrusted\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(adapter::findAll)
                .isInstanceOf(AemetGatewayException.class)
                .hasMessageContaining("URL de datos no válida");
        server.verify();
    }
}
