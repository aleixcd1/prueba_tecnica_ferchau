package es.ferchau.weather.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum TemperatureUnit {
    G_CEL,
    G_FAH;

    public BigDecimal fromCelsius(BigDecimal celsius) {
        BigDecimal value = this == G_FAH
                ? celsius.multiply(BigDecimal.valueOf(9))
                        .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                        .add(BigDecimal.valueOf(32))
                : celsius;
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
