package org.trophic.graph.service;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ITISServiceIT {

    @Test
    public void lookupNonExistentTaxon() throws LSIDLookupServiceException {
        String term = "Bregmacerous contori";
        assertNull(lookupTerm(term));

    }

    // TODO ITIS webservice have changed and no longer return ITIS TSN, email sent on Feb 12, 2013
    @Ignore
    @Test
    public void lookupExistentTaxon() throws LSIDLookupServiceException {
        assertThat(lookupTerm("Fundulus jenkinsi"), is("urn:lsid:itis.gov:itis_tsn:165653"));
    }

    @Test
    public void lookupValidCommonName() throws LSIDLookupServiceException {
        assertThat(lookupTerm("Common Snook"), is(nullValue()));
    }

    @Ignore
    @Test
    public void lookupNA() throws LSIDLookupServiceException {
        assertThat(lookupTerm("NA"), is(nullValue()));
    }

    private String lookupTerm(String term) throws LSIDLookupServiceException {
        return new ITISService().lookupLSIDByTaxonName(term);
    }


}
