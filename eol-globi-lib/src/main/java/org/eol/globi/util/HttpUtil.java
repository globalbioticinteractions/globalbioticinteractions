package org.eol.globi.util;

import com.Ostermiller.util.Base64;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eol.globi.Version;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class HttpUtil {
    private static final Log LOG = LogFactory.getLog(HttpUtil.class);

    public static final int FIVE_MINUTES_IN_MS = 5 * 60 * 1000;
    protected static final String APPLICATION_JSON = "application/json;charset=UTF-8";
    public static final int FIVE_SECONDS = 5000;
    private static CloseableHttpClient httpClient = null;
    private static CloseableHttpClient failFastHttpClient = null;

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient(FIVE_MINUTES_IN_MS);
        }
        return httpClient;
    }

    public static HttpClient getFailFastHttpClient() {
        if (failFastHttpClient == null) {
            RequestConfig config = RequestConfig.custom()
                    .setSocketTimeout(FIVE_SECONDS)
                    .setConnectTimeout(FIVE_SECONDS)
                    .build();

            failFastHttpClient = HttpClientBuilder
                    .create()
                    .disableCookieManagement()
                    .setDefaultRequestConfig(config)
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
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(soTimeoutMs)
                .setConnectTimeout(soTimeoutMs)
                .build();

        return HttpClientBuilder.create()
                .setRetryHandler(new DefaultHttpRequestRetryHandler())
                .setUserAgent("globalbioticinteractions/" + Version.getVersion() + " (https://globalbioticinteractions.org; mailto:info@globalbioticinteractions.org)")
                .setServiceUnavailableRetryStrategy(new CustomServiceUnavailableStrategy())
                .disableCookieManagement()
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

    public static String getRemoteJson(String uri) throws IOException {
        return executeAndRelease(httpGetJson(URI.create(uri)));
    }

    public static String getContent(URI uri) throws IOException {
        return executeAndRelease(new HttpGet(uri));
    }

    public static String getContent(String uri) throws IOException {
        return getContent(URI.create(uri));
    }

    public static String executeAndRelease(HttpGet get) throws IOException {
        return executeAndRelease(get, HttpUtil.getHttpClient());
    }

    public static String executeAndRelease(HttpGet get, HttpClient client) throws IOException {
        return executeAndRelease(get, client, new BasicResponseHandler());
    }

    public static String executeAndRelease(HttpGet get, HttpClient client, ResponseHandler<String> responseHandler) throws IOException {
        try {
            return client.execute(get, responseHandler);
        } catch (IOException ex) {
            throw new IOException("failed to get [" + get.getURI() + "]", ex);
        } finally {
            get.releaseConnection();
        }
    }

    public static String executeWithTimer(HttpRequestBase request, ResponseHandler<String> handler) throws IOException {
        try {
            HttpClient httpClient = getHttpClient();
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            String response = httpClient.execute(request, handler);
            stopwatch.stop();
            if (LOG.isDebugEnabled() && stopwatch.getTime() > 3000) {
                String responseTime = "slowish http request (took " + stopwatch.getTime() + "ms) for [" + request.getURI().toString() + "]";
                LOG.debug(responseTime);
            }

            return response;
        } finally {
            request.releaseConnection();
        }
    }

    public static ResponseHandler<String> createUTF8BasicResponseHandler() {
        return new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse response) throws IOException {
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                if(statusLine.getStatusCode() >= 300) {
                    EntityUtils.consume(entity);
                    throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                } else {
                    return entity == null?null:EntityUtils.toString(entity, StandardCharsets.UTF_8);
                }
            }
        };
    }

    public static HttpGet withBasicAuthHeader(HttpGet request, String username, String password) {
        String encode = Base64.encode(username + ":" + password);
        request.addHeader("Authorization", "Basic " + encode);
        return request;
    }
}
