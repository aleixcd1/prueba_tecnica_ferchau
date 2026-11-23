package es.ferchau.weather.infrastructure.aemet;

import es.ferchau.weather.domain.model.DailyForecast;
import es.ferchau.weather.domain.model.Municipality;
import es.ferchau.weather.domain.model.PrecipitationProbability;
import es.ferchau.weather.domain.port.out.MunicipalityCatalog;
import es.ferchau.weather.domain.port.out.WeatherForecastProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AemetOpenDataAdapter implements MunicipalityCatalog, WeatherForecastProvider {
    private static final List<String> PRECIPITATION_PERIODS =
            List.of("00-06", "06-12", "12-18", "18-24");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final URI baseUri;

    public AemetOpenDataAdapter(RestClient aemetRestClient,
                                ObjectMapper objectMapper,
                                @Value("${aemet.api-key:}") String apiKey,
                                @Value("${aemet.base-url}") String baseUrl) {
        this.restClient = aemetRestClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUri = URI.create(baseUrl);
    }

    @Override
    @Cacheable(cacheNames = "municipalities", key = "'all'", sync = true)
    public List<Municipality> findAll() {
        JsonNode data = requestData("/api/maestro/municipios");
        if (!data.isArray()) {
            throw new AemetGatewayException("AEMET devolvió un listado de municipios no válido");
        }

        List<Municipality> municipalities = new ArrayList<>();
        data.forEach(item -> {
            String id = municipalityCode(item);
            String name = text(item, "nombre");
            if (!id.isBlank() && !name.isBlank()) {
                municipalities.add(new Municipality(id, name, text(item, "provincia")));
            }
        });
        if (municipalities.isEmpty()) {
            throw new AemetGatewayException("AEMET devolvió un listado de municipios vacío");
        }
        return List.copyOf(municipalities);
    }

    @Override
    @Cacheable(cacheNames = "forecasts", key = "#municipalityId + ':' + #date", sync = true)
    public DailyForecast findForecast(String municipalityId, LocalDate date) {
        JsonNode data = requestData("/api/prediccion/especifica/municipio/diaria/" + municipalityId);
        JsonNode municipalityPrediction = data.isArray() ? data.path(0) : data;
        JsonNode days = municipalityPrediction.path("prediccion").path("dia");

        if (!days.isArray()) {
            throw new AemetGatewayException("AEMET no devolvió el desglose diario de la predicción");
        }

        for (JsonNode day : days) {
            if (isDate(day.path("fecha").asText(), date)) {
                return mapForecast(day);
            }
        }
        throw new AemetGatewayException("AEMET no tiene predicción disponible para el día solicitado");
    }

    private JsonNode requestData(String path) {
        if (apiKey.isBlank()) {
            throw new AemetGatewayException("Falta configurar la clave AEMET_API_KEY");
        }

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(path).queryParam("api_key", apiKey).build())
                    .retrieve()
                    .body(String.class);
            JsonNode response = parseJson(responseBody, "AEMET devolvió una respuesta de metadatos vacía");
            if (response == null || !response.path("estado").canConvertToInt()) {
                throw new AemetGatewayException("AEMET devolvió una respuesta de metadatos no válida");
            }
            int status = response.path("estado").asInt();
            if (status == 429) {
                throw new AemetGatewayException(
                        "AEMET ha limitado temporalmente las solicitudes. Inténtalo más tarde.");
            }
            if (status != 200) {
                String description = text(response, "descripcion");
                String detail = description.isBlank() ? "" : ": " + description;
                throw new AemetGatewayException(
                        "AEMET rechazó la solicitud de datos (estado " + status + ")" + detail);
            }

            String dataUrl = response.path("datos").asText("");
            URI resolvedDataUri = URI.create(dataUrl);
            if (!resolvedDataUri.isAbsolute()
                    || !baseUri.getScheme().equalsIgnoreCase(resolvedDataUri.getScheme())
                    || !baseUri.getHost().equalsIgnoreCase(resolvedDataUri.getHost())
                    || resolvedDataUri.getPort() != baseUri.getPort()) {
                throw new AemetGatewayException("AEMET devolvió una URL de datos no válida");
            }

            String dataBody = restClient.get().uri(resolvedDataUri).retrieve().body(String.class);
            return parseJson(dataBody, "AEMET devolvió una respuesta de datos vacía");
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new AemetGatewayException(
                        "AEMET ha limitado temporalmente las solicitudes. Inténtalo más tarde.", exception);
            }
            throw new AemetGatewayException(
                    "AEMET respondió con HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new AemetGatewayException("No se pudo consultar la API de AEMET", exception);
        } catch (IllegalArgumentException exception) {
            throw new AemetGatewayException("AEMET devolvió una URL de datos no válida", exception);
        }
    }

    private JsonNode parseJson(String body, String emptyBodyMessage) {
        if (body == null || body.isBlank()) {
            throw new AemetGatewayException(emptyBodyMessage);
        }
        try {
            return objectMapper.readTree(body);
        } catch (JacksonException exception) {
            throw new AemetGatewayException("AEMET devolvió un documento JSON no válido", exception);
        }
    }

    private DailyForecast mapForecast(JsonNode day) {
        List<BigDecimal> temperatures = new ArrayList<>();
        JsonNode temperatureData = day.path("temperatura");
        if (!temperatureData.isArray()) {
            temperatureData = temperatureData.path("dato");
        }
        temperatureData.forEach(item -> number(item.path("value")).ifPresent(temperatures::add));
        if (temperatures.isEmpty()) {
            throw new AemetGatewayException("AEMET no devolvió temperaturas para el día solicitado");
        }
        BigDecimal average = temperatures.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(temperatures.size()), 2, RoundingMode.HALF_UP);

        List<PrecipitationProbability> precipitation = PRECIPITATION_PERIODS.stream()
                .map(period -> new PrecipitationProbability(
                        aggregateProbability(day.path("probPrecipitacion"), period), period))
                .toList();
        return new DailyForecast(average, precipitation);
    }

    private Integer aggregateProbability(JsonNode source, String targetPeriod) {
        int targetStart = Integer.parseInt(targetPeriod.substring(0, 2));
        int targetEnd = Integer.parseInt(targetPeriod.substring(3, 5));
        BigDecimal weightedProbability = BigDecimal.ZERO;
        int totalHours = 0;

        for (JsonNode item : source) {
            String period = text(item, "periodo");
            String[] bounds = period.split("-");
            if (bounds.length != 2) {
                continue;
            }
            try {
                int start = Integer.parseInt(bounds[0]);
                int end = Integer.parseInt(bounds[1]);
                if (end == 0 && start > 0) {
                    end = 24;
                }
                int overlap = Math.max(0, Math.min(end, targetEnd) - Math.max(start, targetStart));
                var probability = number(item.path("value"));
                if (overlap > 0 && probability.isPresent()) {
                    weightedProbability = weightedProbability.add(
                            probability.get().multiply(BigDecimal.valueOf(overlap)));
                    totalHours += overlap;
                }
            } catch (NumberFormatException ignored) {
                continue;
            }
        }

        return totalHours == 0 ? null : weightedProbability
                .divide(BigDecimal.valueOf(totalHours), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private boolean isDate(String value, LocalDate expected) {
        try {
            return LocalDate.parse(value.substring(0, 10)).equals(expected);
        } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
            return false;
        }
    }

    private String municipalityCode(JsonNode municipality) {
        String id = text(municipality, "id");
        if (id.startsWith("id")) {
            id = id.substring(2);
        }
        if (id.matches("\\d{5}")) {
            return id;
        }
        String oldId = text(municipality, "id_old");
        return oldId.matches("\\d{5}") ? oldId : "";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private java.util.Optional<BigDecimal> number(JsonNode node) {
        if (node.isNumber()) {
            return java.util.Optional.of(node.decimalValue());
        }
        if (node.isTextual()) {
            try {
                return java.util.Optional.of(new BigDecimal(node.asText().trim()));
            } catch (NumberFormatException ignored) {
                return java.util.Optional.empty();
            }
        }
        return java.util.Optional.empty();
    }
}
