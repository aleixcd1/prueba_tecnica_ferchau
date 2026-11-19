package es.ferchau.weather.infrastructure.web;

import es.ferchau.weather.application.service.MunicipalitySearchService;
import es.ferchau.weather.application.service.NextDayForecastService;
import es.ferchau.weather.domain.model.Municipality;
import es.ferchau.weather.domain.model.TemperatureUnit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Weather", description = "Municipios y predicción para el día siguiente")
public class WeatherController {
    private final MunicipalitySearchService municipalitySearchService;
    private final NextDayForecastService nextDayForecastService;

    public WeatherController(MunicipalitySearchService municipalitySearchService,
                             NextDayForecastService nextDayForecastService) {
        this.municipalitySearchService = municipalitySearchService;
        this.nextDayForecastService = nextDayForecastService;
    }

    @GetMapping("/municipalities")
    @Operation(summary = "Busca municipios por nombre", description = "La búsqueda no distingue mayúsculas ni tildes.")
    public List<MunicipalityResponse> searchMunicipalities(
            @Parameter(description = "Nombre o parte del nombre del municipio")
            @RequestParam @Size(max = 80) String query) {
        return municipalitySearchService.search(query).stream()
                .map(MunicipalityResponse::from)
                .toList();
    }

    @GetMapping("/forecast/{municipalityId}")
    @Operation(summary = "Obtiene la predicción del día siguiente")
    public ForecastResponse getNextDayForecast(
            @PathVariable @Pattern(regexp = "\\d{5}") String municipalityId,
            @RequestParam(defaultValue = "G_CEL") TemperatureUnit unit) {
        return ForecastResponse.from(nextDayForecastService.getForecast(municipalityId, unit));
    }
}
