package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class WoRMSServiceIT {

    @Test
    public void lookupExistingSpeciesTaxon() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupIdByName("Peprilus burti"), is("WORMS:276560"));
    }

    @Test
    public void lookupExistentPath() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupTaxonPathById("WORMS:276560"), containsString("Actinopterygii"));
    }

    @Test
    public void lookupExistingGenusTaxon() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupIdByName("Peprilus"), is("WORMS:159825"));
    }

    @Test
    public void lookupNonExistentTaxon() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupIdByName("Brutus blahblahi"), is(nullValue()));
    }

    @Test
    public void lookupNonExistentTaxonPath() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupTaxonPathById("WORMS:EEEEEE"), is(nullValue()));
    }

    @Test
    public void lookupEOLIdTaxonPath() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupTaxonPathById("EOL:123"), is(nullValue()));
    }

    @Test
    public void lookupNA() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupIdByName("NA"), is(nullValue()));
    }

    @Test
    public void lookupNull() throws PropertyEnricherException {
        assertThat(new WoRMSService().lookupIdByName(null), is(nullValue()));
    }


}
