package org.eol.globi.server;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class DietNicheWidthControllerIT extends ITBase {

    @Test
    public void dietaryNicheAriopsisNonExistingPreyTaxon() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Ariopsis?preyTaxon=Non%20Existing";
        String response = HttpClient.httpGet(uri);
        assertThat(response, not(containsString("Ariopsis felis")));
    }

    @Test
    public void dietaryNicheAriopsisTwoPreyTaxa() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Ariopsis?preyTaxon=Mollusca&preyTaxon=Amphipoda";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Ariopsis felis"));
    }

    @Test
    public void dietaryNicheAriopsisOnePreyTaxa() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Ariopsis?preyTaxon=Mollusca";
        String response = HttpClient.httpGet(uri);
        assertThat(response, not(containsString("Ariopsis felis")));
    }

    @Test
    public void dietaryNicheAriopsisFelisMatch() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Ariopsis%20felis";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Ariopsis felis"));
    }

    @Test
    public void dietaryNicheElasmobranchii() throws IOException {
        String uri = getURLPrefix() + "dietNicheWidth/Elasmobranchii";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }
}
