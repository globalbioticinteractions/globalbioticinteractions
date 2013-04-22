package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class CypherProxyControllerTest {

    private static Log LOG = LogFactory.getLog(CypherProxyControllerTest.class);


    @Test
    public void findPrey() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf("Homo sapiens");
        assertThat(list, Is.is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPredatorsOf("Hemiramphus brasiliensis");
        assertThat(list, Is.is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyObservationsOf("Ariopsis felis");
        assertThat(list, Is.is(notNullValue()));
        list = new CypherProxyController().findPreyObservationsOf("Rattus rattus");
        assertThat(list, Is.is(notNullValue()));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPredatorObservationsOf("Rattus rattus");
        assertThat(list, Is.is(notNullValue()));

        list = new CypherProxyController().findPredatorObservationsOf("Ariopsis felis");
        assertThat(list, Is.is(notNullValue()));
    }

    @Test
    public void findTaxon() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findTaxon("Homo sap");
        assertThat(list, Is.is(notNullValue()));
    }

    @Test
    public void findExternalLinkForTaxonWithName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName("Homo sapiens");
        assertThat(externalLink, Is.is("{\"url\":\"http://eol.org/pages/327955\"}"));
    }

}
