package es.ferchau.weather.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record DailyForecast(BigDecimal meanTemperatureCelsius,
                            List<PrecipitationProbability> precipitationProbabilities) {
    public DailyForecast {
        precipitationProbabilities = List.copyOf(precipitationProbabilities);
    }
}
