package org.trophic.graph.worms;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WoRMSServiceTest {

    @Test
    public void lookupExistingSpeciesTaxon() throws IOException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("Peprilus burti");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:276560"));
    }

    @Test
    public void lookupExistingGenusTaxon() throws IOException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("Peprilus");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:159825"));
    }

    @Test
    public void lookupNonExistentTaxon() throws IOException {
        String lsid = new WoRMSService().lookupLSIDByTaxonName("Brutus blahblahi");
        assertThat(lsid, is(nullValue()));

    }


}
