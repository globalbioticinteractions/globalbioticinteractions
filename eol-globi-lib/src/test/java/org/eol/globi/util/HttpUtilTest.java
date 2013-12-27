package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.params.HttpConnectionParams;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HttpUtilTest {

    @Test
    public void timeout() {
        HttpClient httpClient = HttpUtil.createHttpClient();
        Integer soTimeout = HttpConnectionParams.getSoTimeout(httpClient.getParams());
        assertThat(soTimeout, is(greaterThan(0)));
    }

}
