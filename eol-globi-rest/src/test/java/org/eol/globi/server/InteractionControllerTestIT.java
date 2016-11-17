package org.eol.globi.server;

import org.apache.commons.lang3.ArrayUtils;
import org.eol.globi.util.CypherUtil;
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
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, containsString("Homo sapiens"));
    }

    @Test
    public void findPreyExternalId() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "OTT:770315", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, containsString("Homo sapiens"));
    }

    @Test
    public void findThunnusPrey() throws IOException, URISyntaxException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/11
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Thunnus", CypherQueryBuilder.INTERACTION_EATS, null)).execute(null);
        assertThat(list, containsString("Thunnus alalunga"));
        assertThat(list, containsString("Thunnus albacares"));
    }

    @Test
    public void findPreyAtLocation() throws IOException, URISyntaxException {
        HttpServletRequest request = getLocationRequest();
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Homo sapiens", CypherQueryBuilder.INTERACTION_EATS, null)).execute(request);
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

    @Test
    public void findPreyAtLocationNoLongitude() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocationNoLatitude() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = CypherUtil.executeRemote(InteractionController.createQuery(null, CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null));
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTargetsForSource() throws IOException, URISyntaxException {
        String list = CypherUtil.executeRemote(InteractionController.createQuery("Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null));
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
        list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredatorDistinctCSV() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_EATS, null)).execute(request);
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

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(request);
        assertThat(list, allOf(containsString("\"latitude\",\"longitude\""), not(containsString(",null,"))));
    }

    @Test
    public void findPredatorObservationsJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");
        when(request.getParameter("includeObservations")).thenReturn("true");

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_EATS, null)).execute(request);
        assertThat(list, containsString("\"source\":"));
        assertThat(list, containsString("\"target\":"));
        assertThat(list, containsString("\"type\":\"eats\""));
    }

    @Test
    public void findAccordingTo() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");
        when(request.getParameter("sourceTaxon")).thenReturn("json.v2");
        when(request.getParameter("accordingTo")).thenReturn("inaturalist");
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Enhydra"});
                put("accordingTo", new String[]{"inaturalist"});
            }
        });

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request)).execute(request);
        assertThat(list, containsString("\"source\":"));
        assertThat(list, containsString("\"target\":"));
        assertThat(list, containsString("\"type\":\"preysOn\""));
    }

    @Test
    public void findIdPrefix() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxonIdPrefix", new String[]{"EOL"});
                put("sourceTaxon", new String[]{"Quercus"});
                put("interactionType", new String[]{"hasPathogen"});
            }
        });

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request)).execute(request);
        assertThat(list, containsString("Plantae"));
        assertThat(list, containsString("hasPathogen"));
    }

    @Test
    public void findNumberOfStudies() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Quercus"});
                put("interactionType", new String[]{"hasPathogen"});
                put("accordingTo", new String[]{"inaturalist"});
                put("field", new String[]{"source_taxon_name", "interaction_type", "target_taxon_name", "number_of_studies"});
            }
        });

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request)).execute(request);
        assertThat(list, containsString("Quercus"));
        assertThat(list, containsString("hasPathogen"));
    }

    @Test
    public void findNumberOfStudiesWithPrefix() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxonIdPrefix", new String[]{"INAT_TAXON"});
                put("sourceTaxon", new String[]{"Quercus"});
                put("interactionType", new String[]{"hasPathogen"});
                put("accordingTo", new String[]{"inaturalist"});
                put("field", new String[]{"source_taxon_name", "interaction_type", "target_taxon_name", "number_of_studies"});
            }
        });

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request)).execute(request);
        assertThat(list, containsString("Quercus"));
        assertThat(list, containsString("hasPathogen"));
    }

    @Test
    public void findIdPrefixIncludeObservations() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("taxonIdPrefix")).thenReturn("NCBI");
        when(request.getParameter("sourceTaxon")).thenReturn("Quercus");
        when(request.getParameter("includeObservations")).thenReturn("true");
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxonIdPrefix", new String[]{"NCBI"});
                put("sourceTaxon", new String[]{"Quercus"});
                put("includeObservations", new String[]{"true"});
            }
        });

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request)).execute(request);
        assertThat(list, containsString("Viridiplantae"));
    }

    @Test
    public void findPreyOfJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");

        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_EATS, null)).execute(request);
        assertThat(list, allOf(containsString("\"source\":"), containsString("\"target\":"), containsString("\"type\":\"eats\"")));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("includeObservations")).thenReturn("true");


        String list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, null)).execute(request);
        assertThat(list, is(notNullValue()));

        list = new CypherQueryExecutor(new InteractionController().findInteractions(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, null)).execute(request);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "Homo sapiens")).execute(null);
        assertThat(list, is(notNullValue()));

        list = new CypherQueryExecutor(new InteractionController().findInteractions(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, "Rattus rattus")).execute(null);
        assertThat(list, is(notNullValue()));
    }


}
