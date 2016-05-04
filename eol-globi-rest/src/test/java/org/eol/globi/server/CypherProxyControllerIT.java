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
    public void getByCitation() throws IOException {
        String uri = getURLPrefix() + "interaction?accordingTo=Smales%2C%20L%20R.%20%22An%20Annotated%20Checklist%20of%20the%20Australian%20Acanthocephala%20from%20Mammalian%20and%20Bird%20Hosts.%22%20Records%20of%20the%20South%20Australian%20Museum%2036%20%282003%29%3A%2059-82.";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void getByShortCitation() throws IOException {
        String uri = getURLPrefix() + "interaction?accordingTo=gomexsi";
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
