package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.HttpConnectionParams;

public class HttpUtil {

    public static final int FIVE_MINUTES_IN_MS = 5 * 60 * 1000;

    public static HttpClient createHttpClient() {
        return createHttpClient(FIVE_MINUTES_IN_MS);
    }

    public static HttpClient createHttpClient(int soTimeoutMs) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), soTimeoutMs);
        DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(5, true);
        httpClient.setHttpRequestRetryHandler(retryHandler);
        return httpClient;
    }
}
