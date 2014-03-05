package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class CypherProxyControllerTest {

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
    public void findShortestPaths() throws IOException {
        String externalLink = new CypherProxyController().findShortestPaths(null, "Homo sapiens", "Rattus rattus");
        assertThat(externalLink, containsString("Rattus rattus"));
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
