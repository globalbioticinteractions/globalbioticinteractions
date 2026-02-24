package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class HttpUtilTest  {

    @Test
    public void testAgentString() {
        String userAgentString = HttpUtil.getUserAgentString("1.2.3");
        assertThat(userAgentString, Is.is("globalbioticinteractions/1.2.3 (https://globalbioticinteractions.org; mailto:info@globalbioticinteractions.org)"));
    }

}