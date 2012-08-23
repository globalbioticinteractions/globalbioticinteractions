package org.trophic.graph.service;

import org.junit.Test;
import org.trophic.graph.service.WoRMSService;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WoRMSServiceTest {

    @Test
    public void lookupExistingSpeciesTaxon() throws LSIDLookupServiceException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("Peprilus burti");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:276560"));
    }

    @Test
    public void lookupExistingGenusTaxon() throws LSIDLookupServiceException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("Peprilus");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:159825"));
    }

    @Test
    public void lookupNonExistentTaxon() throws LSIDLookupServiceException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("Brutus blahblahi");
        assertThat(lsid, is(nullValue()));

    }


}
