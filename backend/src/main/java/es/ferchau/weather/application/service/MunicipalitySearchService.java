package es.ferchau.weather.application.service;

import es.ferchau.weather.domain.model.Municipality;
import es.ferchau.weather.domain.port.out.MunicipalityCatalog;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class MunicipalitySearchService {
    private final MunicipalityCatalog municipalityCatalog;

    public MunicipalitySearchService(MunicipalityCatalog municipalityCatalog) {
        this.municipalityCatalog = municipalityCatalog;
    }

    public List<Municipality> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return municipalityCatalog.findAll().stream()
                .filter(municipality -> normalize(municipality.name()).contains(normalizedQuery))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
