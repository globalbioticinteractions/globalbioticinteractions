package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.AutoRetryHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.client.cache.ExponentialBackOffSchedulingStrategy;
import org.apache.http.params.HttpConnectionParams;

public class HttpUtil {

    public static final int FIVE_MINUTES_IN_MS = 5 * 60 * 1000;

    public static HttpClient createHttpClient() {
        return createHttpClient(FIVE_MINUTES_IN_MS);
    }

    public static HttpClient createHttpClient(int soTimeoutMs) {
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(soTimeoutMs)
                .setConnectTimeout(soTimeoutMs)
                .build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(10, 5 * 1000))
                .setDefaultRequestConfig(config)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(5, true));
        return httpClientBuilder.build();
    }
}
