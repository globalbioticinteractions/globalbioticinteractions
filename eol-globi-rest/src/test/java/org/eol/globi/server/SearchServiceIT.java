package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class SearchServiceIT extends ITBase {

    public static final String COLUMN_PREFIX = "\"columns\":[\"(taxon.name)\", \"(taxon.commonNames)\", \"(taxon.path)\"]";

    @Test
    public void findCloseMatchesTypo() throws IOException {
        assertHuman("Homo%20SApient");
    }

    @Test
    public void findCloseMatchesTypo2() throws IOException {
        assertHuman("Homo%20SApientz");
    }

    @Test
    public void findCloseMatchesPartial() throws IOException {
        assertHuman("Homo%20sa");
        assertHuman("Homo%20sapi");
    }

    @Test
    public void findCloseMatchesShortPartial() throws IOException {
        assertHuman("Homo%20s");
    }

    private void assertHuman(String searchTerm) throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/" + searchTerm;
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("Homo sapiens"));
        assertThat(response, containsString("man"));
    }

    @Test
    @Ignore(value = "should work after ensuring that lower case terms are indexed")
    public void findCloseMatchesShortPartial2() throws IOException {
        assertHuman("h%20s");
    }

    @Test
    public void findCloseMatchesCommonNameFoxDutch() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/vos";
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("vos"));
    }

    @Test
    public void findCloseMatchesCommonNameFoxFrenchType() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/reinard";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Vulpes vulpes"));
        assertThat(response, containsString("renard"));
    }

    @Test
    public void findCloseMatchesScientificNameRedFoxWithTypo() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Vulpes%20vules";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Vulpes vulpes"));
    }

    @Test
    public void findCloseMatchesScientificGenus() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Ariidae";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Ariopsis felis"));
    }

    @Test
    public void findCloseMatchesScientificChineseCharacters() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Ariidae";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("海鲇科"));
    }

    @Test
    public void findCloseMatchesLowerCase() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/King%20mackerel";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Scomberomorus cavalla"));
        assertThat(response, containsString("king mackeral"));
    }

    @Test
    public void findCloseMatchesUpperCase() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/King%20Mackerel";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Scomberomorus cavalla"));
        assertThat(response, containsString("king mackeral"));
    }

    @Ignore("see issue #40")
    @Test
    public void ensureSingleMatch() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Ariopsis%20felis";
        String response = HttpClient.httpGet(uri);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        assertThat(jsonNode.get("data").get(0).get(0).getTextValue(), is("Ariopsis felis"));
        assertThat(jsonNode.get("data").size(), is(1));
    }

    @Test
    public void findTaxon() throws IOException {
        String uri = getURLPrefix() + "findTaxon/Ariopsis%20felis";
        String response = HttpClient.httpGet(uri);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        assertThat(jsonNode.get("name").getTextValue(), is("Ariopsis felis"));
        assertThat(jsonNode.get("path").getTextValue(), containsString("Actinopterygii"));
        assertThat(jsonNode.get("commonNames").getTextValue(), containsString("hardhead catfish"));
        assertThat(jsonNode.get("externalId").getTextValue(), containsString(":"));
    }

}
