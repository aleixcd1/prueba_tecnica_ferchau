package es.ferchau.weather.application.model;

import es.ferchau.weather.domain.model.PrecipitationProbability;
import es.ferchau.weather.domain.model.TemperatureUnit;

import java.math.BigDecimal;
import java.util.List;

public record ForecastSummary(BigDecimal meanTemperature,
                             TemperatureUnit temperatureUnit,
                             List<PrecipitationProbability> precipitationProbabilities) {
    public ForecastSummary {
        precipitationProbabilities = List.copyOf(precipitationProbabilities);
    }
}
