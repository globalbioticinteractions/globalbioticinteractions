package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ITISServiceIT {

    @Test
    public void lookupNonExistentTaxon() throws PropertyEnricherException {
        String term = "Bregmacerous contori";
        assertNull(lookupTerm(term));

    }

    @Test
    public void lookupExistentTaxon() throws PropertyEnricherException {
        String actual = lookupTerm("Fundulus jenkinsi");
        assertThat(actual, is("ITIS:165653"));
    }

    @Test
    public void lookupPathByExistingTaxon() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        String s = itisService.lookupPropertyValueByTaxonName("Fundulus jenkinsi", PropertyAndValueDictionary.PATH);
        assertThat(s, is(notNullValue()));
        assertThat(s, containsString("Animalia"));
    }

    @Test
    public void lookupPathByNonExistingTaxon() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        String s = itisService.lookupPropertyValueByTaxonName("donald duck", PropertyAndValueDictionary.PATH);
        assertThat(s, is (nullValue()));
    }

    @Test
    public void lookupValidCommonName() throws PropertyEnricherException {
        assertThat(lookupTerm("Common Snook"), is(nullValue()));
    }

    @Ignore
    @Test
    public void lookupNA() throws PropertyEnricherException {
        assertThat(lookupTerm("NA"), is(nullValue()));
    }

    private String lookupTerm(String term) throws PropertyEnricherException {
        return new ITISService().lookupIdByName(term);
    }


}
