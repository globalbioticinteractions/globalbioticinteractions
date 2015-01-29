package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.CypherQuery;
import org.junit.Test;
import org.junit.internal.matchers.StringContains;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class TaxonSearchImplTest {

    @Test
    public void nameSuggestions() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("homo zapiens");
        String result = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(result, containsString("Homo sapiens"));
    }

    public static final String COLUMN_PREFIX = "{\n  \"columns\" : [ \"taxon.name\", \"taxon.commonNames\", \"taxon.path\" ]";


    @Test
    public void findCloseMatchesTypo() throws IOException {
        assertHuman("Homo SApient");
    }

    @Test
    public void findCloseMatchesTypo2() throws IOException {
        assertHuman("Homo SApientz");
    }

    @Test
    public void findCloseMatchesPartial() throws IOException {
        assertHuman("Homo sa");
        assertHuman("Homo sapi");
    }

    @Test
    public void findCloseMatchesShortPartial() throws IOException {
        assertHuman("Homo s");
    }

    private void assertHuman(String searchTerm) throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew(searchTerm);
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response.startsWith(COLUMN_PREFIX), is(true));
        assertThat(response, StringContains.containsString("Homo sapiens"));
        assertThat(response, StringContains.containsString("man"));
    }

    @Test
    public void findCloseMatchesShortPartial2() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("h s");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response.startsWith(COLUMN_PREFIX), is(true));
        // expect at least one common name
        assertThat(response, StringContains.containsString("@en"));
    }

    @Test
    public void findCloseMatchesCommonNameFoxDutch() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("vos");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response.startsWith(COLUMN_PREFIX), is(true));
        assertThat(response, StringContains.containsString("vos"));
    }

    @Test
    public void findCloseMatchesCommonNameFoxFrenchType() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("reinard");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response, StringContains.containsString("Vulpes vulpes"));
        assertThat(response, StringContains.containsString("renard"));
    }

    @Test
    public void findCloseMatchesScientificNameRedFoxWithTypo() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("Vulpes vules");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response, StringContains.containsString("Vulpes vulpes"));
    }

    @Test
    public void findCloseMatchesScientificGenus() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("Ariidae");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        JsonNode mapper = new ObjectMapper().readTree(response);
        JsonNode data = mapper.get("data");
        assertThat(data.isArray(), is(true));
        assertThat(data.size() > 0, is (true));
        assertThat(response, StringContains.containsString("Ariidae"));
    }

    @Test
    public void findCloseMatchesScientificChineseCharacters() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("Ariidae");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response, StringContains.containsString("海鱨"));
    }

    @Test
    public void findCloseMatchesLowerCase() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("King mackerel");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response, StringContains.containsString("Scomberomorus cavalla"));
        assertThat(response, StringContains.containsString("king mackeral"));
    }

    @Test
    public void findCloseMatchesUpperCase() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("King Mackerel");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        assertThat(response, StringContains.containsString("Scomberomorus cavalla"));
        assertThat(response, StringContains.containsString("king mackeral"));
    }

    @Test
    public void ensureSingleMatch() throws IOException {
        CypherQuery cypherQuery = new TaxonSearchImpl().findCloseMatchesForCommonAndScientificNamesNew("Ariopsis felis");
        String response = new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        assertThat(jsonNode.get("data").get(0).get(0).getTextValue(), is("Ariopsis felis"));
        assertThat(jsonNode.get("data").size(), is(1));
    }

    @Test
    public void findTaxonProxy() throws IOException {
        String response = new TaxonSearchImpl().findTaxonProxy("Ariopsis felis", null);
        JsonNode resp = new ObjectMapper().readTree(response);
        JsonNode columns = resp.get("columns");
        assertThat(columns.get(0).getTextValue(), is("name"));
        assertThat(columns.get(1).getTextValue(), is("commonNames"));
        assertThat(columns.get(2).getTextValue(), is("path"));
        assertThat(columns.get(3).getTextValue(), is("externalId"));
        JsonNode info = resp.get("data").get(0);
        assertThat(info.get(0).getTextValue(), is("Ariopsis felis"));
        assertThat(info.get(1).getTextValue(), StringContains.containsString("catfish"));
        assertThat(info.get(2).getTextValue(), StringContains.containsString("Actinopterygii"));
        assertThat(info.get(3).getTextValue(), StringContains.containsString(":"));
    }

    @Test
    public void findTaxonAriopsisFelis() throws IOException {
        Map<String, String> props = new TaxonSearchImpl().findTaxon("Ariopsis felis", null);
        assertThat(props.get("name"), is("Ariopsis felis"));
        assertThat(props.get("commonNames"), StringContains.containsString("catfish"));
        assertThat(props.get("path"), StringContains.containsString("Actinopterygii"));
        assertThat(props.get("externalId"), StringContains.containsString(":"));
    }

    @Test
    public void findTaxonByExternalId() throws IOException {
        Map<String, String> props = new TaxonSearchImpl().findTaxon("Ariopsis felis", null);
        assertThat(props.get("name"), is("Ariopsis felis"));
        assertThat(props.get("commonNames"), StringContains.containsString("catfish"));
        assertThat(props.get("path"), StringContains.containsString("Actinopterygii"));
        assertThat(props.get("externalId"), StringContains.containsString(":"));
    }
}
