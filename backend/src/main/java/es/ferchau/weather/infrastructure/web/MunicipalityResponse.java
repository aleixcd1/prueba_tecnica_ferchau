package es.ferchau.weather.infrastructure.web;

import es.ferchau.weather.domain.model.Municipality;

public record MunicipalityResponse(String id, String nombre, String provincia) {
    public static MunicipalityResponse from(Municipality municipality) {
        return new MunicipalityResponse(municipality.id(), municipality.name(), municipality.province());
    }
}
