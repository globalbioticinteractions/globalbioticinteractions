package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLServiceIT {

    @Test
    public void lookupByName() throws LSIDLookupServiceException {
        assertThat(lookupPageIdByScientificName("Homo sapiens"), is("EOL:327955"));
        assertThat(lookupPageIdByScientificName("Puccinia caricina var. ribesii-pendulae"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Hesperocharis paranensis"), is("EOL:176594"));
        // TODO need to find a way to include only pages that have at least one external taxonomy
        assertThat(lookupPageIdByScientificName("Dead roots"), is("EOL:19665069"));

    }

    @Test
    public void lookupByNameYieldsMoreThanOneMatches() throws LSIDLookupServiceException {
        // this species has two matches  http://eol.org/27383107 and http://eol.org/209714, first is picked
        assertThat(lookupPageIdByScientificName("Copadichromis insularis"), is("EOL:27383107"));

        // below matches both http://eol.org/4443282 and http://eol.org/310363, but first is picked
        assertThat(lookupPageIdByScientificName("Spilogale putorius gracilis"), is("EOL:4443282"));

        // etc
        assertThat(lookupPageIdByScientificName("Crocethia alba"), is("EOL:4373991"));
        assertThat(lookupPageIdByScientificName("Ecdyonuridae"), is("EOL:19663261"));
        assertThat(lookupPageIdByScientificName("Catasticta hegemon"), is("EOL:173526"));
        assertThat(lookupPageIdByScientificName("Theridion ovatum"), is("EOL:3180388"));
        assertThat(lookupPageIdByScientificName("Cambarus propinquus"), is("EOL:31345356"));
        assertThat(lookupPageIdByScientificName("Zoanthus flosmarinus"), is("EOL:13029466"));
        assertThat(lookupPageIdByScientificName("Vellidae"), is("EOL:644"));
        assertThat(lookupPageIdByScientificName("Mylothris spica"), is("EOL:180160"));
        assertThat(lookupPageIdByScientificName("Mylothris rueppellii"), is("EOL:180170"));

    }

    @Test
    public void lookupByNameYieldsNoMatches() throws LSIDLookupServiceException {
        assertThat(lookupPageIdByScientificName("Clio acicula"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Aegires oritzi"), is(nullValue()));
    }

    private String lookupPageIdByScientificName(String taxonName) throws LSIDLookupServiceException {
        return new EOLService().lookupLSIDByTaxonName(taxonName);
    }

}
