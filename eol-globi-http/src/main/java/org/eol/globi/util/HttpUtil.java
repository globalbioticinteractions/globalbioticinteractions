package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eol.globi.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class HttpUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtil.class);

    public static final int TIMEOUT_DEFAULT = 60 * 1000;
    protected static final String APPLICATION_JSON = "application/json;charset=UTF-8";
    public static final int TIMEOUT_SHORT = 5000;
    private static CloseableHttpClient httpClient = null;
    private static CloseableHttpClient failFastHttpClient = null;

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient(TIMEOUT_DEFAULT);
        }
        return httpClient;
    }

    public static HttpClient getFailFastHttpClient() {
        if (failFastHttpClient == null) {
            RequestConfig config = RequestConfig.custom()
                    .setSocketTimeout(TIMEOUT_SHORT)
                    .setConnectTimeout(TIMEOUT_SHORT)
                    .setCircularRedirectsAllowed(true)
                    .build();

            failFastHttpClient = HttpClientBuilder
                    .create()
                    .disableCookieManagement()
                    .setDefaultRequestConfig(config)
                    // for loading proxy config see https://github.com/globalbioticinteractions/nomer/issues/121
                    .useSystemProperties()
                    .build();
        }
        return failFastHttpClient;
    }

    // should only be called once
    public static void shutdown() {
        if (HttpUtil.httpClient != null) {
            closeQuietly(HttpUtil.httpClient);
            HttpUtil.httpClient = null;
        }
        if (HttpUtil.failFastHttpClient != null) {
            closeQuietly(HttpUtil.failFastHttpClient);
            HttpUtil.failFastHttpClient = null;
        }
    }

    protected static void closeQuietly(CloseableHttpClient httpClient) {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    protected static CloseableHttpClient createHttpClient(int soTimeoutMs) {
        HttpClientBuilder httpClientBuilder = createHttpClientBuilder(soTimeoutMs);
        return httpClientBuilder
                .build();
    }

    public static HttpClientBuilder createHttpClientBuilder(int soTimeoutMs) {
        RequestConfig config = RequestConfig
                .custom()
                .setSocketTimeout(soTimeoutMs)
                .setConnectTimeout(soTimeoutMs)
                .setCircularRedirectsAllowed(true)
                // see https://stackoverflow.com/questions/54591140/apache-http-client-stop-removing-double-slashes-from-url
                .setNormalizeUri(false)
                .build();

        return HttpClientBuilder
                .create()
                .setRetryHandler(new DefaultHttpRequestRetryHandler())
                .setUserAgent("globalbioticinteractions/" + Version.getVersion() + " (https://globalbioticinteractions.org; mailto:info@globalbioticinteractions.org)")
                .setServiceUnavailableRetryStrategy(new CustomServiceUnavailableStrategy())
                .disableCookieManagement()
               // for loading proxy config from system properties see https://github.com/globalbioticinteractions/nomer/issues/121
                .useSystemProperties()
                .setDefaultRequestConfig(config);
    }

    public static HttpGet httpGetJson(URI uri) {
        HttpGet httpGet = new HttpGet(uri);
        addJsonHeaders(httpGet);
        return httpGet;
    }

    public static void addJsonHeaders(HttpRequestBase httpGet) {
        httpGet.setHeader("Accept", APPLICATION_JSON);
        httpGet.setHeader("Content-Type", APPLICATION_JSON);
        httpGet.setHeader("X-Stream", "true");
    }

    public static HttpGet addAcceptHeader(HttpGet httpGet) {
        httpGet.setHeader("Accept", "*/*");
        return httpGet;
    }

    public static String getRemoteJson(String uri) throws IOException {
        return executeAndRelease(httpGetJson(URI.create(uri)));
    }

    public static String getContent(URI uri) throws IOException {
        return executeAndRelease(addAcceptHeader(new HttpGet(uri)));
    }

    public static String getContent(String uri) throws IOException {
        return getContent(URI.create(uri));
    }

    private static String executeAndRelease(HttpGet get) throws IOException {
        return executeAndRelease(get, HttpUtil.getHttpClient());
    }

    public static String executeAndRelease(HttpGet get, HttpClient client) throws IOException {
        return executeAndRelease(get, client, new BasicResponseHandler());
    }

    private static String executeAndRelease(HttpGet get, HttpClient client, ResponseHandler<String> responseHandler) throws IOException {
        try {
            return client.execute(get, responseHandler);
        } catch (IOException ex) {
            throw new IOException("failed to get [" + get.getURI() + "]", ex);
        } finally {
            get.releaseConnection();
        }
    }
    
}
