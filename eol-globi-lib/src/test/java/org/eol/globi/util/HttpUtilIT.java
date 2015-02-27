package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class HttpUtilIT {

    @Test(expected = ConnectTimeoutException.class)
    public void timeoutVeryShort() throws IOException {
        HttpClient httpClient = HttpUtil.createHttpClient(1);
        executeRequest(httpClient);
    }

    @Test
    public void timeoutDefault() throws IOException {
        HttpClient httpClient = HttpUtil.createHttpClient();
        executeRequest(httpClient);
    }

    private void executeRequest(HttpClient httpClient) throws IOException {
        HttpGet get = new HttpGet("http://eol.org");
        httpClient.execute(get, new BasicResponseHandler());
    }
}
