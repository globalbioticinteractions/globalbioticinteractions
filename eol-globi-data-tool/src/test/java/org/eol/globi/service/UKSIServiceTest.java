package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UKSIServiceTest {

    private UKSIService uksiService;

    @Before
    public void init() {
        uksiService = new UKSIService();
    }

    @Test
    public void lookupNameWithCorrection() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        uksiService.lookupPropertiesByName("Stellaria apetala", properties);
        assertThat(properties.get(Taxon.NAME), is("Stellaria pallida"));
        assertThat(properties.get(Taxon.EXTERNAL_ID), is("UKSI:NBNSYS0000157226"));
    }

    @Test
    public void lookupNameNoCorrectionButPresent() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        uksiService.lookupPropertiesByName("Serpulidae", properties);
        assertThat(properties.get(Taxon.NAME), is("Serpulidae"));
        assertThat(properties.get(Taxon.EXTERNAL_ID), is("UKSI:NBNSYS0000177931"));
    }

    @Test
    public void lookupNameNoCorrectionNotPresent() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        uksiService.lookupPropertiesByName("Yogi the Bear", properties);
        assertThat(properties.get(Taxon.NAME), is(nullValue()));
        assertThat(properties.get(Taxon.EXTERNAL_ID), is(nullValue()));
    }

}
