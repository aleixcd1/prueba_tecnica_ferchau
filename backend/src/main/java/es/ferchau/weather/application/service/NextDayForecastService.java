package es.ferchau.weather.application.service;

import es.ferchau.weather.application.model.ForecastSummary;
import es.ferchau.weather.domain.model.TemperatureUnit;
import es.ferchau.weather.domain.port.out.WeatherForecastProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class NextDayForecastService {
    private static final ZoneId SPANISH_TIME_ZONE = ZoneId.of("Europe/Madrid");

    private final WeatherForecastProvider forecastProvider;
    private final Clock clock;

    public NextDayForecastService(WeatherForecastProvider forecastProvider, Clock clock) {
        this.forecastProvider = forecastProvider;
        this.clock = clock;
    }

    public ForecastSummary getForecast(String municipalityId, TemperatureUnit unit) {
        TemperatureUnit requestedUnit = Objects.requireNonNullElse(unit, TemperatureUnit.G_CEL);
        LocalDate tomorrow = LocalDate.now(clock.withZone(SPANISH_TIME_ZONE)).plusDays(1);
        var forecast = forecastProvider.findForecast(municipalityId, tomorrow);

        return new ForecastSummary(
                requestedUnit.fromCelsius(forecast.meanTemperatureCelsius()),
                requestedUnit,
                forecast.precipitationProbabilities());
    }
}
