package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WoRMSServiceIT {

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

    @Test
    public void lookupNA() throws LSIDLookupServiceException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("NA");
        assertThat(lsid, is(nullValue()));
    }


}
