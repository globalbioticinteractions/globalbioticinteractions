package org.eol.globi.server;

import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class CypherProxyControllerIT extends ITBase {

    @Test
    public void findExternalUrl() throws IOException {
        String uri = getURLPrefix() + "findExternalUrlForTaxon/Homo%20sapiens";
        assertThat(HttpUtil.getRemoteJson(uri), containsString("url"));
    }

    @Test
    public void getGoMexSILocations() throws IOException {
        String uri = getURLPrefix() + "locations?accordingTo=gomexsi";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void getAllLocations() throws IOException {
        String uri = getURLPrefix() + "locations";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }


}
