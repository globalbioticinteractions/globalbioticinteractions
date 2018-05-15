package org.eol.globi.util;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HttpUtilIT {

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
}
