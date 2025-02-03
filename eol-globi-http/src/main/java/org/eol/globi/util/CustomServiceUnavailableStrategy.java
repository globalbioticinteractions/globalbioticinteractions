package org.eol.globi.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

import java.util.Arrays;
import java.util.List;

public class CustomServiceUnavailableStrategy implements ServiceUnavailableRetryStrategy {
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_INTERVAL_MS = 5 * 1000;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final List<Integer> RETRY_ERROR_CODES = Arrays.asList(HttpStatus.SC_SERVICE_UNAVAILABLE, TOO_MANY_REQUESTS);

    @Override
    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        return executionCount <= MAX_RETRIES &&
                RETRY_ERROR_CODES.contains(response.getStatusLine().getStatusCode());
    }

    @Override
    public long getRetryInterval() {
        return RETRY_INTERVAL_MS;
    }
}
