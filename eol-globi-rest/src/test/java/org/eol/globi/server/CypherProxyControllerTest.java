package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class CypherProxyControllerTest {

    private static Log LOG = LogFactory.getLog(CypherProxyControllerTest.class);


    @Test
    public void findPrey() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(null, "Homo sapiens");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocation() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(getRequest(), "Homo sapiens");
        assertThat(list, is(notNullValue()));
    }

    private HttpServletRequest getRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("lat", new String[]{"18.24829"});
                put("lng", new String[]{"-66.49989"});
            }
        });
        return request;
    }

    @Test
    public void findPreyAtLocationNoLongitude() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(null, "Homo sapiens");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocationNoLatitude() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(null, "Homo sapiens");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPredatorsOf(null, "Hemiramphus brasiliensis");
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyObservationsOf(null, "Ariopsis felis");
        assertThat(list, is(notNullValue()));
        list = new CypherProxyController().findPreyObservationsOf(null, "Rattus rattus");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPredatorObservationsOf(null, "Rattus rattus");
        assertThat(list, is(notNullValue()));

        list = new CypherProxyController().findPredatorObservationsOf(null, "Ariopsis felis");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTaxon() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findTaxon("Homo sap");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findExternalLinkForTaxonWithName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName("Homo sapiens");
        assertThat(externalLink, is("{\"url\":\"http://eol.org/pages/327955\"}"));
    }

    @Test
    public void findInteractions() throws IOException {
        String externalLink = new CypherProxyController().findInteractions(getRequest());
        assertThat(externalLink, containsString("ATE"));
    }

    @Test
    public void findShortestPaths() throws IOException {
        String externalLink = new CypherProxyController().findShortestPaths("Homo sapiens", "Rattus rattus");
        assertThat(externalLink, containsString("Rattus rattus"));
    }

}
