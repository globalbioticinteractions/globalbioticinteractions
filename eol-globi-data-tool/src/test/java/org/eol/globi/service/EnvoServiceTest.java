package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EnvoServiceTest {

    @Test
    public void firstTerm() throws TaxonPropertyLookupServiceException {
        EnvoService envoService = new EnvoService();
        assertThat(envoService.lookupIdByName("Sediment"), is("ENVO:00002007"));
        assertThat(envoService.lookupIdByName("sediment"), is("ENVO:00002007"));
        assertThat(envoService.lookupIdByName("soil"), is("ENVO:00001998"));
        assertThat(envoService.lookupIdByName("Soil"), is("ENVO:00001998"));
        assertThat(envoService.lookupIdByName("something else"), is(nullValue()));

        assertThat(envoService.lookupTaxonPathById("ENVO:00002007"), is("environmental material | sediment"));
        assertThat(envoService.lookupTaxonPathById(""), is(nullValue()));
        assertThat(envoService.lookupTaxonPathById(null), is(nullValue()));
    }
}
