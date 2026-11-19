package es.ferchau.weather.domain.port.out;

import es.ferchau.weather.domain.model.Municipality;

import java.util.List;

public interface MunicipalityCatalog {
    List<Municipality> findAll();
}
