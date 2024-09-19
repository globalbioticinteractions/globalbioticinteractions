package org.eol.globi.util;

import org.apache.http.HttpVersion;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class CustomServiceUnavailableStrategyTest {

    @Test
    public void firstRetry() {
        CustomServiceUnavailableStrategy strategy = new CustomServiceUnavailableStrategy();
        boolean shouldRetry = strategy.retryRequest(new BasicHttpResponse(
                        new BasicStatusLine(HttpVersion.HTTP_1_1, 429, "too many requests")),
                1,
                new HttpClientContext()
        );

        assertThat(shouldRetry, Is.is(true));
        assertThat(strategy.getRetryInterval(), Is.is(5000L));
    }

    @Test
    public void secondRetry() {
        CustomServiceUnavailableStrategy strategy = new CustomServiceUnavailableStrategy();
        boolean shouldRetry = strategy.retryRequest(new BasicHttpResponse(
                        new BasicStatusLine(HttpVersion.HTTP_1_1, 429, "too many requests")),
                2,
                new HttpClientContext()
        );

        assertThat(shouldRetry, Is.is(true));
        assertThat(strategy.getRetryInterval(), Is.is(5000L));
    }

    @Test
    public void thirdRetry() {
        CustomServiceUnavailableStrategy strategy = new CustomServiceUnavailableStrategy();
        BasicHttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1,
                        429,
                        "too many requests")
        );

        boolean shouldRetry = strategy.retryRequest(response,
                3,
                new HttpClientContext()
        );
        assertThat(shouldRetry, Is.is(true));
        assertThat(strategy.getRetryInterval(), Is.is(5000L));
    }

    @Test
    public void eleventhRetry() {
        CustomServiceUnavailableStrategy strategy = new CustomServiceUnavailableStrategy();
        boolean shouldRetry = strategy.retryRequest(new BasicHttpResponse(
                        new BasicStatusLine(HttpVersion.HTTP_1_1, 429, "too many requests")),
                11,
                new HttpClientContext()
        );

        assertThat(shouldRetry, Is.is(false));
    }

}