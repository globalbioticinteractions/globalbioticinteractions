package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ITISServiceIT {

    @Test
    public void lookupNonExistentTaxon() throws TaxonPropertyLookupServiceException {
        String term = "Bregmacerous contori";
        assertNull(lookupTerm(term));

    }

    @Test
    public void lookupExistentTaxon() throws TaxonPropertyLookupServiceException {
        String actual = lookupTerm("Fundulus jenkinsi");
        assertThat(actual, is("urn:lsid:itis.gov:itis_tsn:165653"));
    }

    @Test
    public void lookupPathByExistingTaxon() throws TaxonPropertyLookupServiceException {
        ITISService itisService = new ITISService();
        String s = itisService.lookupPropertyValueByTaxonName("Fundulus jenkinsi", Taxon.PATH);
        // note that ITISService doesn't support path lookup (yet)
        assertThat(s, is (nullValue()));
    }

    @Test
    public void lookupPathByNonExistingTaxon() throws TaxonPropertyLookupServiceException {
        ITISService itisService = new ITISService();
        String s = itisService.lookupPropertyValueByTaxonName("donald duck", Taxon.PATH);
        assertThat(s, is (nullValue()));
    }

    @Test
    public void lookupValidCommonName() throws TaxonPropertyLookupServiceException {
        assertThat(lookupTerm("Common Snook"), is(nullValue()));
    }

    @Ignore
    @Test
    public void lookupNA() throws TaxonPropertyLookupServiceException {
        assertThat(lookupTerm("NA"), is(nullValue()));
    }

    private String lookupTerm(String term) throws TaxonPropertyLookupServiceException {
        return new ITISService().lookupIdByName(term);
    }


}
