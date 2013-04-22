package org.eol.globi.server;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class CypherProxyControllerIT {

    @Test
    public void ping() throws IOException {
        String uri = getURLPrefix() + "ping";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPreyForPredator() throws IOException {
        String uri = getURLPrefix() + "predator/Homo%20sapiens/listPrey";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void findTaxonUsingPartialString() throws IOException {
        String uri = getURLPrefix() + "findTaxon/Homo%20sap";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Homo sapiens"));
    }

    @Test
    public void findExternalUrl() throws IOException {
        String uri = getURLPrefix() + "findExternalUrlForTaxon/Homo%20sapiens";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("url"));
    }

    @Test
    public void listContributors() throws IOException {
        long responseTimeForFirstCall = timedContributorRequest();
        long responseTimeForSecondCall = timedContributorRequest();
        assertThat("expected second call to be two orders of magnitude smaller due to caching, but found response time of second call to be [" + responseTimeForSecondCall + "] ms", responseTimeForSecondCall < (responseTimeForFirstCall / 100), is(true));
    }

    private long timedContributorRequest() throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        requestContributors();
        stopWatch.stop();
        return stopWatch.getTime();
    }

    private void requestContributors() throws IOException {
        String uri = getURLPrefix() + "contributors";
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Roopnarine"));
    }

    protected String getURLPrefix() {
        return "http://localhost:8080/";
    }

}
