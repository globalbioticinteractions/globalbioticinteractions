package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;

public abstract class BaseHttpClientService {

    private static final Log LOG = LogFactory.getLog(BaseHttpClientService.class);

    private HttpClient httpClient;

    public BaseHttpClientService() {
        this.httpClient = HttpUtil.createHttpClient();
    }

    public void shutdown() {
        if (this.httpClient != null) {
            this.httpClient.getConnectionManager().shutdown();
            this.httpClient = null;
        }
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            this.httpClient = HttpUtil.createHttpClient();
        }
        return httpClient;
    }

    protected String execute(HttpUriRequest request, ResponseHandler<String> handler) throws IOException {
        HttpClient httpClient = getHttpClient();
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        String response = httpClient.execute(request, handler);
        stopwatch.stop();
        if (stopwatch.getTime() > 3000) {
            String responseTime = "slowish http request (took " + stopwatch.getTime() + "ms) for [" + request.getURI().toString() + "]";
            LOG.warn(responseTime);
        }

        return response;
    }

}
