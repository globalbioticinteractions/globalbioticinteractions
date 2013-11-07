package org.eol.globi.server;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class SearchServiceIT extends ITBase {

    public static final String COLUMN_PREFIX = "\"columns\":[\"(taxon.name)\", \"(taxon.commonNames)\", \"(taxon.path)\"]";

    @Test
    public void findCloseMatchesTypo() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Homo%20SApient";
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("Homo sapiens"));
        assertThat(response, containsString("human"));
    }

    @Test
    public void findCloseMatchesTypo2() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Homo%20SApientz";
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("Homo sapiens"));
        assertThat(response, containsString("human"));
    }

    @Test
    public void findCloseMatchesPartial() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Homo%20sa";
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("Homo sapiens"));
        assertThat(response, containsString("man"));
    }

    @Test
    public void findCloseMatchesShortPartial() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Homo%20s";
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("Homo sapiens"));
        assertThat(response, containsString("man"));
    }

    @Test
    @Ignore(value="should work after ensuring that lower case terms are indexed")
    public void findCloseMatchesShortPartial2() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/h%20s";
        String response = HttpClient.httpGet(uri);
        assertThat(response.startsWith("{" + COLUMN_PREFIX), is(true));
        assertThat(response, containsString("Homo sapiens"));
        assertThat(response, containsString("man"));
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

}
