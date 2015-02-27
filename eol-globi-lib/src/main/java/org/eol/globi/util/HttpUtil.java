package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.URI;

public class HttpUtil {

    public static final int FIVE_MINUTES_IN_MS = 5 * 60 * 1000;
    protected static final String APPLICATION_JSON = "application/json;charset=UTF-8";
    private static CloseableHttpClient httpClient = null;

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient(FIVE_MINUTES_IN_MS);
        }
        return httpClient;
    }

    // should only be called once
    public static void shutdown() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    protected static CloseableHttpClient createHttpClient(int soTimeoutMs) {
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(soTimeoutMs)
                .setConnectTimeout(soTimeoutMs)
                .build();

        return HttpClientBuilder.create()
                .setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(10, 5 * 1000))
                .setDefaultRequestConfig(config).build();
    }

    public static HttpGet httpGetJson(String uri) {
        HttpGet httpGet = new HttpGet(uri);
        addJsonHeaders(httpGet);
        return httpGet;
    }

    public static void addJsonHeaders(HttpRequestBase httpGet) {
        httpGet.setHeader("Accept", APPLICATION_JSON);
        httpGet.setHeader("Content-Type", APPLICATION_JSON);
    }

    public static String getRemoteJson(String uri) throws IOException {
        return executeAndRelease(httpGetJson(uri));
    }

    public static String getContent(URI uri) throws IOException {
        return executeAndRelease(new HttpGet(uri));
    }

    public static String getContent(String uri) throws IOException {
        return executeAndRelease(new HttpGet(uri));
    }

    protected static String executeAndRelease(HttpGet get) throws IOException {
        try {
            return HttpUtil.getHttpClient().execute(get, new BasicResponseHandler());
        } finally {
            get.releaseConnection();
        }
    }
}
