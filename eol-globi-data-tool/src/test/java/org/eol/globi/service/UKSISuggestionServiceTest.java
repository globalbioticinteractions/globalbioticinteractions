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
    public void lookupNameWithCorrection() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Stellaria apetala");
        uksiSuggestionService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Stellaria pallida"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is("UKSI:NBNSYS0000157226"));
    }

    @Test
    public void lookupNameWithSuggestion() throws PropertyEnricherException {
        assertThat(uksiSuggestionService.suggest("Stellaria apetala"), is("Stellaria pallida"));
        assertThat(uksiSuggestionService.suggest("Bombus"), is("Bombus"));
        assertThat(uksiSuggestionService.suggest("Actinopterygii"), is("Actinopterygii"));
        assertThat(uksiSuggestionService.suggest("Fish"), is("Pisces"));
    }

    @Test
    public void lookupNameWithConflictingSuggestions() throws PropertyEnricherException {
        assertThat(uksiSuggestionService.suggest("Mimesa bicolor"), is("Mimesa equestris"));
        assertThat(uksiSuggestionService.suggest("Mimesa equestris"), is("Mimesa bicolor"));
        assertThat(uksiSuggestionService.suggest("Exidia glandulosa"), is("Exidia plana"));
        assertThat(uksiSuggestionService.suggest("Exidia plana"), is("Exidia glandulosa"));
    }

    @Test
    public void lookupNameNoCorrectionButPresent() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Serpulidae");
        uksiSuggestionService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Serpulidae"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is("UKSI:NBNSYS0000177931"));
    }

    @Test
    public void noSuggestion() {
        assertThat(uksiSuggestionService.suggest("Yogi the Bear"), is("Yogi the Bear"));
    }

    @Test
    public void lookupNameNoCorrectionNotPresent() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Yogi the Bear");
        uksiSuggestionService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Yogi the Bear"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
    }

}
