package es.ferchau.weather.application.service;

import es.ferchau.weather.domain.model.DailyForecast;
import es.ferchau.weather.domain.model.TemperatureUnit;
import es.ferchau.weather.domain.port.out.WeatherForecastProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class NextDayForecastServiceTest {
    @Test
    void requestsTheNextSpanishCalendarDayAndConvertsTheTemperature() {
        AtomicReference<LocalDate> requestedDate = new AtomicReference<>();
        WeatherForecastProvider provider = (municipalityId, date) -> {
            requestedDate.set(date);
            return new DailyForecast(new BigDecimal("20.0"), List.of());
        };
        Clock clock = Clock.fixed(Instant.parse("2026-03-28T23:30:00Z"), ZoneOffset.UTC);
        NextDayForecastService service = new NextDayForecastService(provider, clock);

        var summary = service.getForecast("28079", TemperatureUnit.G_FAH);

        assertThat(requestedDate.get()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(summary.meanTemperature()).isEqualTo(new BigDecimal("68.0"));
        assertThat(summary.temperatureUnit()).isEqualTo(TemperatureUnit.G_FAH);
    }

    @Test
    void defaultsToCelsiusWhenTheUnitIsOmitted() {
        WeatherForecastProvider provider = (municipalityId, date) ->
                new DailyForecast(new BigDecimal("18.26"), List.of());
        NextDayForecastService service = new NextDayForecastService(
                provider, Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC));

        var summary = service.getForecast("28079", null);

        assertThat(summary.meanTemperature()).isEqualTo(new BigDecimal("18.3"));
        assertThat(summary.temperatureUnit()).isEqualTo(TemperatureUnit.G_CEL);
    }
}
