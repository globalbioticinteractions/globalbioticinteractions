package org.eol.globi.server;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class SearchServiceIT extends ITBase {

    public static final String COLUMN_PREFIX = "\"columns\":[\"(taxon.name)\", \"(taxon.commonNames)\", \"(taxon.path)\"]";

    @Test
    public void findCloseMatches() throws IOException {
        String uri = getURLPrefix() + "findCloseMatchesForTaxon/Homo%20SApient";
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
        assertThat(response, containsString("Vulpes vulpes"));
        assertThat(response, containsString("Vos"));
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

}
