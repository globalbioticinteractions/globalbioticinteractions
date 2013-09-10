package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WoRMSServiceIT {

    @Test
    public void lookupExistingSpeciesTaxon() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupIdByName("Peprilus burti");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:276560"));
    }

    @Test
    public void lookupExistingGenusTaxon() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupIdByName("Peprilus");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:159825"));
    }

    @Test
    public void lookupNonExistentTaxon() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupIdByName("Brutus blahblahi");
        assertThat(lsid, is(nullValue()));

    }

    @Test
    public void lookupNA() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupIdByName("NA");
        assertThat(lsid, is(nullValue()));
    }


}
