package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TaxonEnricherImplTest {

    private static TaxonEnricherImpl taxonEnricher;

    @BeforeClass
    public static void init() {
        taxonEnricher = new TaxonEnricherImpl();
    }

    @AfterClass
    public static void destroy() {
        taxonEnricher.shutdown();
    }

    @Test
    public void enrichNoServices() throws PropertyEnricherException {
        List<PropertyEnricher> list = new ArrayList<PropertyEnricher>();
        taxonEnricher.setServices(list);
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Homo sapiens");
            }
        };
        Map<String, String> enrich = taxonEnricher.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
    }

    @Test
    public void enrichTwoService() throws PropertyEnricherException {
        PropertyEnricher serviceA = Mockito.mock(PropertyEnricher.class);
        PropertyEnricher serviceB = Mockito.mock(PropertyEnricher.class);
        enrich("Homo sapiens", serviceA, serviceB);
        verify(serviceA).enrich(anyMap());
        verify(serviceB).enrich(anyMap());
    }

    @Test
    public void enrichTwoServiceOneBlowsUp() throws PropertyEnricherException {
        PropertyEnricher serviceA = Mockito.mock(PropertyEnricher.class);
        PropertyEnricher serviceB = Mockito.mock(PropertyEnricher.class);
        when(serviceB.enrich(anyMap())).thenThrow(PropertyEnricherException.class);

        PropertyEnricher enricher = createEnricher(serviceA, serviceB);

        enrich("Homo sapiens", enricher);
        enrich("Homo sapiens", enricher);
        verify(serviceA, times(2)).enrich(anyMap());
        verify(serviceB, times(2)).enrich(anyMap());

    }

    @Test
    public void enrichTwoServiceFirstComplete() throws PropertyEnricherException {
        PropertyEnricher serviceA = new PropertyEnricher() {

            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                properties = new HashMap<>(properties);
                properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "FIRST:123");
                properties.put(PropertyAndValueDictionary.PATH, "one | two | three");
                properties.put(PropertyAndValueDictionary.COMMON_NAMES, "four | five | six");
                return properties;
            }

            @Override
            public void shutdown() {

            }
        };
        PropertyEnricher serviceB = Mockito.mock(PropertyEnricher.class);

        Taxon taxon = enrich("Homo sapiens", serviceA, serviceB);
        assertThat(taxon.getExternalId(), is("FIRST:123"));
        assertThat(taxon.getPath(), is("one | two | three"));
        assertThat(taxon.getCommonNames(), is("four | five | six"));
        assertThat(taxon.getName(), is("Homo sapiens"));
        verifyZeroInteractions(serviceB);
    }

    private Taxon enrich(final String taxonName, PropertyEnricher serviceA, PropertyEnricher serviceB) throws PropertyEnricherException {
        PropertyEnricher enricher = createEnricher(serviceA, serviceB);
        return enrich(taxonName, enricher);
    }

    private Taxon enrich(final String taxonName, PropertyEnricher enricher) throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, taxonName);
            }
        };
        Taxon enrichedTaxon = new TaxonImpl();
        TaxonUtil.mapToTaxon(enricher.enrich(properties), enrichedTaxon);
        return enrichedTaxon;
    }

    private PropertyEnricher createEnricher(PropertyEnricher serviceA, PropertyEnricher serviceB) {
        TaxonEnricherImpl enricher = taxonEnricher;
        List<PropertyEnricher> list = new ArrayList<PropertyEnricher>();
        list.add(serviceA);
        list.add(serviceB);
        enricher.setServices(list);
        return enricher;
    }

}
