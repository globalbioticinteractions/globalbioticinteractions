package org.eol.globi.util;

import com.Ostermiller.util.Base64;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class HttpUtilIT {

    private static HttpGet withBasicAuthHeader(HttpGet request, String username, String password) {
        String encode = Base64.encode(username + ":" + password);
        request.addHeader("Authorization", "Basic " + encode);
        return request;
    }

    @Test(expected = ConnectTimeoutException.class)
    public void timeoutVeryShort() throws IOException {
        HttpClient httpClient = HttpUtil.createHttpClient(1);
        executeRequest(httpClient);
    }

    @Test
    public void timeoutDefault() throws IOException {
        HttpClient httpClient = HttpUtil.getHttpClient();
        executeRequest(httpClient);
    }

    @Ignore("not yet implemented")
    @Test
    public void cachingForNonCacheable() throws IOException {
        String nonCacheableUri = "http://eol.org";
        assertCaching(nonCacheableUri);
    }

    @Ignore("not yet implemented")
    @Test
    public void cachingForCacheable() throws IOException {
        String nonCacheableUri = "http://media.eol.org/content/2009/07/24/04/21692_98_68.jpg";
        assertCaching(nonCacheableUri);
    }

    protected void assertCaching(String nonCacheableUri) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        HttpClient httpClient = HttpUtil.getHttpClient();
        httpClient.execute(new HttpGet(nonCacheableUri), new BasicResponseHandler());
        stopWatch.stop();
        long firstDelay = stopWatch.getTime();
        stopWatch.reset();
        stopWatch.start();
        HttpUtil.getHttpClient().execute(new HttpGet(nonCacheableUri), new BasicResponseHandler());
        long secondDelay = stopWatch.getTime();
        stopWatch.stop();
        assertThat("expected second delay [" + secondDelay + "] ms to be at least 10x shorter than first [" + firstDelay + "] ms",
                secondDelay, is(lessThan(firstDelay / 10)));
    }

    private void executeRequest(HttpClient httpClient) throws IOException {
        HttpGet get = new HttpGet("http://eol.org");
        httpClient.execute(get, new BasicResponseHandler());
    }

    @Test
    public void withBasicAuthHeaders() throws IOException {
        HttpClientBuilder builder = HttpUtil.createHttpClientBuilder(HttpUtil.TIMEOUT_SHORT);
        CloseableHttpClient httpClient = builder.build();
        String username = "aladdin";
        String password = "opensesame";
        HttpGet get = new HttpGet("http://httpbin.org/basic-auth/aladdin/opensesame");
        withBasicAuthHeader(get, username, password);

        Header[] authorizations = get.getHeaders("Authorization");
        // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization
        assertThat(authorizations[0].getValue(), is("Basic YWxhZGRpbjpvcGVuc2VzYW1l"));
        httpClient.execute(get, new BasicResponseHandler());
    }

}
