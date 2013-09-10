package org.eol.globi.service;

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
        assertThat(lookupTerm("Fundulus jenkinsi"), is("urn:lsid:itis.gov:itis_tsn:165653"));
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
