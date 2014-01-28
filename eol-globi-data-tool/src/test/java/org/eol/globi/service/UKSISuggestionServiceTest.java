package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UKSISuggestionServiceTest {

    private static UKSISuggestionService uksiSuggestionService;

    @BeforeClass
    public static void init() {
        uksiSuggestionService = new UKSISuggestionService();
    }

    @Test
    public void lookupNameWithCorrection() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        uksiSuggestionService.lookupPropertiesByName("Stellaria apetala", properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Stellaria pallida"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is("UKSI:NBNSYS0000157226"));
    }

    @Test
    public void lookupNameWithSuggestion() throws TaxonPropertyLookupServiceException {
        assertThat(uksiSuggestionService.suggest("Stellaria apetala"), is("Stellaria pallida"));
        assertThat(uksiSuggestionService.suggest("Bombus"), is("Bombus"));
        assertThat(uksiSuggestionService.suggest("Actinopterygii"), is("Actinopterygii"));
        assertThat(uksiSuggestionService.suggest("Fish"), is("Pisces"));
    }

    @Test
    public void lookupNameWithSuggestions2() throws TaxonPropertyLookupServiceException {

    }

    @Test
    public void lookupNameNoCorrectionButPresent() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        uksiSuggestionService.lookupPropertiesByName("Serpulidae", properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Serpulidae"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is("UKSI:NBNSYS0000177931"));
    }

    @Test
    public void noSuggestion() {
        assertThat(uksiSuggestionService.suggest("Yogi the Bear"), is("Yogi the Bear"));
    }

    @Test
    public void lookupNameNoCorrectionNotPresent() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        uksiSuggestionService.lookupPropertiesByName("Yogi the Bear", properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is(nullValue()));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
    }

}
