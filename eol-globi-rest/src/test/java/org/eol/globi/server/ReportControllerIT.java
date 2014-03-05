package org.eol.globi.server;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ReportControllerIT extends ITBase {

    @Test
    public void listContributors() throws IOException {
        long responseTimeForFirstCall = timedContributorRequest(null);
        long responseTimeForSecondCall = timedContributorRequest(null);
        assertThat("expected second call to be two orders of magnitude smaller due to caching, but found response time of second call to be [" + responseTimeForSecondCall + "] ms", responseTimeForSecondCall < (responseTimeForFirstCall / 100), is(true));
    }

    @Test
    public void listContributorsSPIRE() throws IOException {
        long responseTimeForFirstCall = timedContributorRequest("SPIRE");
        long responseTimeForSecondCall = timedContributorRequest("SPIRE");
        assertThat("expected second call to be two orders of magnitude smaller due to caching, but found response time of second call to be [" + responseTimeForSecondCall + "] ms", responseTimeForSecondCall < (responseTimeForFirstCall / 100), is(true));
    }

    @Test
    public void listInfo() throws IOException {
        long responseTimeForFirstCall = timedSourcesRequest(null);
        long responseTimeForSecondCall = timedSourcesRequest(null);
        assertThat("expected second call to be two orders of magnitude smaller due to caching, but found response time of second call to be [" + responseTimeForSecondCall + "] ms", responseTimeForSecondCall < (responseTimeForFirstCall / 100), is(true));
    }

    @Test
    public void listInfoSPIRE() throws IOException {
        long responseTimeForFirstCall = timedSourcesRequest("SPIRE");
        long responseTimeForSecondCall = timedSourcesRequest("SPIRE");
        assertThat("expected second call to be two orders of magnitude smaller due to caching, but found response time of second call to be [" + responseTimeForSecondCall + "] ms", responseTimeForSecondCall < (responseTimeForFirstCall / 100), is(true));
    }

    private long timedSourcesRequest(String source) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String uri = getURLPrefix() + "info";
        if (StringUtils.isNotBlank(source)) {
            uri += "?source=" + source;
        }
        HttpClient.httpGet(uri);
        stopWatch.stop();
        return stopWatch.getTime();
    }

    private long timedContributorRequest(String source) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        requestContributors(source);
        stopWatch.stop();
        return stopWatch.getTime();
    }

    private void requestContributors(String source) throws IOException {
        String uri = getURLPrefix() + "contributors";
        if (StringUtils.isNotBlank(source)) {
            uri += "?source=" + source;
        }
        String response = HttpClient.httpGet(uri);
        assertThat(response, containsString("Roopnarine"));
    }
}
