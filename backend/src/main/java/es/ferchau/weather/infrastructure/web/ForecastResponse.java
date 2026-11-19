package es.ferchau.weather.infrastructure.web;

import es.ferchau.weather.application.model.ForecastSummary;
import es.ferchau.weather.domain.model.TemperatureUnit;

import java.math.BigDecimal;
import java.util.List;

public record ForecastResponse(BigDecimal mediaTemperatura,
                               TemperatureUnit unidadTemperatura,
                               List<PrecipitationResponse> probPrecipitacion) {
    public static ForecastResponse from(ForecastSummary forecast) {
        return new ForecastResponse(
                forecast.meanTemperature(),
                forecast.temperatureUnit(),
                forecast.precipitationProbabilities().stream().map(PrecipitationResponse::from).toList());
    }
}
