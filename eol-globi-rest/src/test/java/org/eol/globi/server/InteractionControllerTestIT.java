package org.eol.globi.server;

import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.server.util.ResultFields;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class InteractionControllerTestIT {

    @Test
    public void findPrey() throws IOException, URISyntaxException {
        String list = new InteractionController().findInteractions(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, containsString("Homo sapiens"));
    }

    @Ignore("not yet implemented")
    @Test
    public void findPreyExternalId() throws IOException, URISyntaxException {
        String list = new InteractionController().findInteractions(null, "OTT:770315", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, containsString("Homo sapiens"));
    }

    @Ignore(value = "requires specific datasets on a remote server")
    @Test
    public void findThunnusPrey() throws IOException, URISyntaxException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/11
        String list = new InteractionController().findInteractions(null, "Thunnus", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, containsString("Thunnus alalunga"));
        assertThat(list, containsString("Thunnus albacares"));
    }

    @Test
    public void findPreyAtLocation() throws IOException, URISyntaxException {
        String list = new InteractionController().findInteractions(getLocationRequest(), "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
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
        String list = new InteractionController().findInteractions(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocationNoLatitude() throws IOException, URISyntaxException {
        String list = new InteractionController().findInteractions(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = new InteractionController().findDistinctTaxonInteractions(null, CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTargetsForSource() throws IOException, URISyntaxException {
        String list = new InteractionController().findDistinctTaxonInteractions("Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null).execute(null);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new InteractionController().findInteractions(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
        list = new InteractionController().findInteractions(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredatorDistinctCSV() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");

        String list = new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
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

        String list = new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, allOf(containsString("\"latitude\",\"longitude\""), not(containsString(",null,"))));
    }

    @Test
    public void findPredatorObservationsJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");
        when(request.getParameter("includeObservations")).thenReturn("true");

        String list = new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, containsString("\"source\":"));
        assertThat(list, containsString("\"target\":"));
        assertThat(list, containsString("\"type\":\"preysOn\""));
    }

    @Test
    public void findPreyOfJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");

        String list = new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON);
        assertThat(list, allOf(containsString("\"source\":"), containsString("\"target\":"), containsString("\"type\":\"preysOn\"")));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("includeObservations")).thenReturn("true");


        String list = new InteractionController().findInteractions(request, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY);
        assertThat(list, is(notNullValue()));

        list = new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorPreyObservations() throws IOException, URISyntaxException {
        String list = new InteractionController().findInteractions(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "Homo sapiens");
        assertThat(list, is(notNullValue()));

        list = new InteractionController().findInteractions(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, "Rattus rattus");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findInteractions() throws IOException {
        String externalLink = new InteractionController().findInteractions(getLocationRequest());
        assertThat(externalLink, containsString("ATE"));
        assertThat(externalLink, containsString(ResultFields.SOURCE_TAXON_PATH));
        assertThat(externalLink, containsString(ResultFields.TARGET_TAXON_PATH));
    }

    @Test
    public void findInteractionsBox() throws IOException {
        String externalLink = new InteractionController().findInteractions(getLocationBoxRequest());
        assertThat(externalLink, containsString("ATE"));
        assertThat(externalLink, containsString(ResultFields.SOURCE_TAXON_PATH));
        assertThat(externalLink, containsString(ResultFields.TARGET_TAXON_PATH));
    }

    @Test
    public void findSupportedInteractionTypes() throws IOException {
        String interactionTypes = new InteractionController().getInteractionTypes();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(interactionTypes);
        for (JsonNode interactionType : jsonNode) {
            assertThat(interactionType.has("source"), is(true));
            assertThat(interactionType.has("target"), is(true));
        }
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_HOST_OF), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_PARASITE_OF), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_PREYED_UPON_BY), is(true));
        assertThat(jsonNode.has(CypherQueryBuilder.INTERACTION_PREYS_ON), is(true));
    }


}
