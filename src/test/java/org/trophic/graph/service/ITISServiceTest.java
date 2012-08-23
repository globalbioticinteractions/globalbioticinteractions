package org.trophic.graph.service;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ITISServiceTest {

    @Test
    public void lookupNonExistentTaxon() throws LSIDLookupServiceException {
        String term = "Bregmacerous contori";
        assertNull(lookupTerm(term));

    }

    @Test
    public void lookupExistentTaxon() throws LSIDLookupServiceException {
        assertThat(lookupTerm("Fundulus jenkinsi"), is("urn:lsid:itis.gov:itis_tsn:165653"));


    }

    @Test
    public void lookupValidCommonName() throws LSIDLookupServiceException {
        String lsid = lookupTerm("Common Snook");
        assertThat(lsid, is("urn:lsid:itis.gov:itis_tsn:167648"));

    }

    private String lookupTerm(String term) throws LSIDLookupServiceException {
        return new ITISService().lookupLSIDByTaxonName(term);
    }


}
