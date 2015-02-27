package org.eol.globi.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;

import java.io.IOException;

public class HttpUtil {

    public static final int FIVE_MINUTES_IN_MS = 5 * 60 * 1000;
    protected static final String APPLICATION_JSON = "application/json;charset=UTF-8";

    public static CloseableHttpClient createHttpClient() {
        return createHttpClient(FIVE_MINUTES_IN_MS);
    }

    public static CloseableHttpClient createHttpClient(int soTimeoutMs) {
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(soTimeoutMs)
                .setConnectTimeout(soTimeoutMs)
                .build();

        return HttpClientBuilder.create()
                .setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(10, 5 * 1000))
                .setDefaultRequestConfig(config).build();
    }

    public static String httpGet(String uri) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        addJsonHeaders(httpGet);
        return createHttpClient().execute(httpGet, new BasicResponseHandler());
    }

    public static void addJsonHeaders(HttpRequestBase httpGet) {
        httpGet.setHeader("Accept", APPLICATION_JSON);
        httpGet.setHeader("Content-Type", APPLICATION_JSON);
    }
}
