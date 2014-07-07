package org.eol.globi.server;

import org.eol.globi.util.HttpClient;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class CypherProxyControllerIT extends ITBase {

    @Test
    public void findExternalUrl() throws IOException {
        String uri = getURLPrefix() + "findExternalUrlForTaxon/Homo%20sapiens";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("url"));
    }


}
