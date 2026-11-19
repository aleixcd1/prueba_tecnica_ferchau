package es.ferchau.weather.application.service;

import es.ferchau.weather.domain.model.Municipality;
import es.ferchau.weather.domain.port.out.MunicipalityCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MunicipalitySearchServiceTest {
    private final MunicipalityCatalog catalog = mock(MunicipalityCatalog.class);
    private final MunicipalitySearchService service = new MunicipalitySearchService(catalog);

    @Test
    void findsNamesWithoutMatchingAccentsOrCase() {
        when(catalog.findAll()).thenReturn(List.of(
                new Municipality("15030", "A Coruña", "A Coruña"),
                new Municipality("28079", "Madrid", "Madrid")));

        assertEquals(List.of("A Coruña"), service.search("CORUNA").stream()
                .map(Municipality::name).toList());
    }

    @Test
    void doesNotLoadCatalogForBlankQueries() {
        assertEquals(List.of(), service.search("  "));
        verify(catalog, never()).findAll();
    }
}
