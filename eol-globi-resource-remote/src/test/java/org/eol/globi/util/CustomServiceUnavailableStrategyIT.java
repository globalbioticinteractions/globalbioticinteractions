package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class CustomServiceUnavailableStrategyIT {

    @Test
    public void zenodo() throws IOException {
        Map<String, Long> rateLimits = new TreeMap<>();


        CloseableHttpClient client = HttpClientBuilder
                .create()
                .setServiceUnavailableRetryStrategy(new CustomServiceUnavailableStrategy())
                .build();

        CloseableHttpResponse execute = client.execute(new HttpGet(URI.create("https://zenodo.org/api/records?all_versions=false&q=alternate.identifier:%22urn%3Alsid%3Abiodiversitylibrary.org%3Apart%3A49687%22")));

        Header[] allHeaders = execute.getAllHeaders();
        for (Header allHeader : allHeaders) {
            HeaderElement[] elements = allHeader.getElements();
            for (HeaderElement element : elements) {
                Header[] headers = execute.getHeaders(element.getName());
                for (Header header : headers) {
                    HeaderElement[] elements1 = header.getElements();
                    if (StringUtils.startsWith(element.getName(), "X-RateLimit")) {
                        rateLimits.put(element.getName(), Long.parseLong(header.getValue()));
                    }
                }
            }
        }

        assertThat(execute.getStatusLine().getStatusCode(), Is.is(200));

        assertThat(rateLimits.size(), Is.is(3));

        assertThat(rateLimits.keySet(), hasItems("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset"));

    }

}