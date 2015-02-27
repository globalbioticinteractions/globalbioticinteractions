package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;

public abstract class BaseHttpClientService {

    private static final Log LOG = LogFactory.getLog(BaseHttpClientService.class);

    public void shutdown() {

    }

    protected String execute(HttpRequestBase request, ResponseHandler<String> handler) throws IOException {
        try {
            HttpClient httpClient = HttpUtil.getHttpClient();
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            String response = httpClient.execute(request, handler);
            stopwatch.stop();
            if (stopwatch.getTime() > 3000) {
                String responseTime = "slowish http request (took " + stopwatch.getTime() + "ms) for [" + request.getURI().toString() + "]";
                LOG.warn(responseTime);
            }

            return response;
        } finally {
            request.releaseConnection();
        }
    }

}
