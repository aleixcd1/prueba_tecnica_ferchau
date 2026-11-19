package es.ferchau.weather.infrastructure.web;

import es.ferchau.weather.domain.model.PrecipitationProbability;

public record PrecipitationResponse(Integer probabilidad, String periodo) {
    public static PrecipitationResponse from(PrecipitationProbability probability) {
        return new PrecipitationResponse(probability.probability(), probability.period());
    }
}
