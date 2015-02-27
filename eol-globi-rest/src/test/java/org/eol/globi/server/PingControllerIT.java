package org.eol.globi.server;

import org.apache.http.client.methods.HttpHead;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PingControllerIT extends ITBase {

    @Test
    public void ping() throws IOException {
        String uri = getURLPrefix() + "ping";
        assertThat(HttpUtil.getRemoteJson(uri), is(not(nullValue())));
    }

    @Test
    public void head() throws IOException {
        HttpUtil.getHttpClient().execute(new HttpHead("http://localhost:8080/taxon/Homo%20sapiens/preysOn"));
    }

}
