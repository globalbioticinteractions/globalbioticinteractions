package org.eol.globi.server;

import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class CypherProxyControllerTest {

    @Test
    public void findPrey() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findThunnusPrey() throws IOException, URISyntaxException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/11
        String list = new CypherProxyController().findPreyOf(null, "Thunnus", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, containsString("Thunnus alalunga"));
        assertThat(list, containsString("Thunnus albacares"));
    }

    @Test
    public void findPreyAtLocation() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(getLocationRequest(), "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    private HttpServletRequest getLocationRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("lat", new String[]{"18.24829"});
                put("lng", new String[]{"-66.49989"});
            }
        });
        return request;
    }

    private HttpServletRequest getLocationBoxRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("nw_lat", new String[]{"18.34"});
                put("nw_lng", new String[]{"-66.50"});
                put("se_lat", new String[]{"18.14"});
                put("se_lng", new String[]{"-66.48"});
            }
        });
        return request;
    }


    @Test
    public void findPreyAtLocationNoLongitude() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocationNoLatitude() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findPreyOf(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTaxonInteractions(null, CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTargetsForSource() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTaxonInteractions("Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null).execute(null);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findObservationsOf(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
        list = new CypherProxyController().findObservationsOf(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredatorDistinctCSV() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");

        String list = new CypherProxyController().findPreyOf(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        String[] rows = list.split("\n");
        String[] rows_no_header = ArrayUtils.remove(rows, 0);
        assertThat(rows_no_header.length > 0, is(true));

        for (String row : rows_no_header) {
            String[] columns = row.split("\",");
            assertThat(columns[0], is("\"Ariopsis felis"));
            assertThat(columns.length, is(3));
        }
    }

    @Test
    public void findPredatorObservationsCSV() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String>() {
            {
                put("includeObservations", "true");
                put("type", "csv");
            }
        });
        when(request.getParameter("type")).thenReturn("csv");
        when(request.getParameter("includeObservations")).thenReturn("true");

        String list = new CypherProxyController().findObservationsOf(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, allOf(containsString("\"latitude\",\"longitude\""), not(containsString(",null,"))));
    }

    @Test
    public void findPredatorObservationsJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");
        when(request.getParameter("includeObservations")).thenReturn("true");

        String list = new CypherProxyController().findObservationsOf(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, allOf(containsString("\"source\":"),
                containsString("\"target\":"),
                containsString("\"latitude\":"),
                containsString("\"longitude\":"),
                containsString("\"type\":\"preysOn\"")));
    }

    @Test
    public void findPreyOfJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");

        String list = new CypherProxyController().findPreyOf(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, allOf(containsString("\"source\":"), containsString("\"target\":"), containsString("\"type\":\"preysOn\"")));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("includeObservations")).thenReturn("true");


        String list = new CypherProxyController().findObservationsOf(request, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY);
        assertThat(list, is(notNullValue()));

        list = new CypherProxyController().findObservationsOf(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findObservationsOf(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "Homo sapiens");
        assertThat(list, is(notNullValue()));

        list = new CypherProxyController().findObservationsOf(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, "Rattus rattus");
        assertThat(list, is(notNullValue()));
    }

    @Ignore(value = "this assumes an externally running system")
    @Test
    public void findExternalLinkForTaxonWithName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName(null, "Homo sapiens");
        assertThat(externalLink, is("{\"url\":\"http://eol.org/pages/327955\"}"));
    }

    @Test
    public void findExternalLinkForTaxonWithNonExistingName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName(null, "This aint exist yet");
        assertThat(externalLink, is("{}"));
    }


    @Test
    public void findExternalLinkForNonExistingStudyWithTitle() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForStudyWithTitle(null, "None existing study");
        assertThat(externalLink, is("{}"));
    }


    @Test
    public void findInteractions() throws IOException {
        String externalLink = new CypherProxyController().findInteractions(getLocationRequest());
        // TODO re-enable assertThat(externalLink, containsString("ATE"));
        assertThat(externalLink, containsString(ResultFields.SOURCE_TAXON_PATH));
        assertThat(externalLink, containsString(ResultFields.TARGET_TAXON_PATH));
    }

    @Test
    public void findInteractionsBox() throws IOException {
        String externalLink = new CypherProxyController().findInteractions(getLocationBoxRequest());
        // TODO re-enable assertThat(externalLink, containsString("ATE"));
        assertThat(externalLink, containsString(ResultFields.SOURCE_TAXON_PATH));
        assertThat(externalLink, containsString(ResultFields.TARGET_TAXON_PATH));
    }

    @Test
    public void findShortestPaths() throws IOException {
        String externalLink = new CypherProxyController().findShortestPaths(null, "Homo sapiens", "Rattus rattus");
        assertThat(externalLink, containsString("Rattus rattus"));
    }


    @Test
    public void findSupportedInteractionTypes() throws IOException {
        String interactionTypes = new CypherProxyController().getInteractionTypes();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(interactionTypes);
        for (JsonNode interactionType : jsonNode) {
            assertThat(interactionType.has("source"), is(true));
            assertThat(interactionType.has("target"), is(true));
        }
    }

    @Test
    public void sources() throws IOException {
        String studies = new CypherProxyController().sources();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).size() > 0, is(true));
        assertThat(data.get(0).get(0).getValueAsText(), not(is(nullValue())));
    }


    @Test
    public void info() throws IOException {
        String studies = new CypherProxyController().info(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getValueAsInt() > 0, is(true));
    }

    @Test
    public void infoBySource() throws IOException {
        String studies = new CypherProxyController().info("http://gomexsi.tamucc.edu");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getValueAsInt() > 0, is(true));
    }


}
