package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class WoRMSServiceTest {

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
    public void lookupByUnacceptedTaxonName() throws PropertyEnricherException {
        String wormsId = new WoRMSService().lookupIdByName("Sterrhurus concavovesiculus");
        assertThat(wormsId, is("WORMS:729172"));
        HashMap<String, String> properties = new HashMap<String, String>();
        Map<String, String> enriched = new WoRMSService().enrichById(wormsId, properties);
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), containsString("Lecithochirium"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), containsString("WORMS:726834"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), containsString("Lecithochirium concavovesiculus"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), containsString("Trematoda"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), containsString("WORMS:19948"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), containsString("subfamily | genus | species"));
    }

    @Test
    public void lookupByAcceptedTaxonName() throws PropertyEnricherException {
        String wormsId = new WoRMSService().lookupIdByName("Gadus morhua");
        HashMap<String, String> properties = new HashMap<String, String>();
        Map<String, String> enriched = new WoRMSService().enrichById(wormsId, properties);
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Atlantic cod @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("bacalao @es"));
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
