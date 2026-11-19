package es.ferchau.weather.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemperatureUnitTest {
    @Test
    void convertsCelsiusToFahrenheitAndRoundsToOneDecimal() {
        assertEquals(new BigDecimal("66.7"),
                TemperatureUnit.G_FAH.fromCelsius(new BigDecimal("19.25")));
    }

    @Test
    void keepsCelsiusInTheRequiredOutputPrecision() {
        assertEquals(new BigDecimal("19.3"),
                TemperatureUnit.G_CEL.fromCelsius(new BigDecimal("19.25")));
    }
}
