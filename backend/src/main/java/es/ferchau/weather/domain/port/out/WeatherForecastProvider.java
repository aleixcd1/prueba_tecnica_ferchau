package es.ferchau.weather.domain.port.out;

import es.ferchau.weather.domain.model.DailyForecast;

import java.time.LocalDate;

public interface WeatherForecastProvider {
    DailyForecast findForecast(String municipalityId, LocalDate date);
}
