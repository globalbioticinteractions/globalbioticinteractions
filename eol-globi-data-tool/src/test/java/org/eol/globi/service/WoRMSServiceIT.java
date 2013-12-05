package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class WoRMSServiceIT {

    @Test
    public void lookupExistingSpeciesTaxon() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupIdByName("Peprilus burti");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:276560"));
    }

    @Test
    public void lookupExistentPath() throws TaxonPropertyLookupServiceException {
        String path = new WoRMSService().lookupTaxonPathById("urn:lsid:marinespecies.org:taxname:276560");
        assertThat(path, containsString("Actinopterygii"));
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
    public void lookupNonExistentTaxonPath() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupTaxonPathById("urn:lsid:marinespecies.org:taxname:EEEEEE");
        assertThat(lsid, is(nullValue()));
    }

    @Test
    public void lookupEOLIdTaxonPath() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupTaxonPathById("EOL:123");
        assertThat(lsid, is(nullValue()));
    }

    @Test
    public void lookupNA() throws TaxonPropertyLookupServiceException {
        String lsid = new WoRMSService().lookupIdByName("NA");
        assertThat(lsid, is(nullValue()));
    }


}
