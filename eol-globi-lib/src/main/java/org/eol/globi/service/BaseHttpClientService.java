package org.eol.globi.service;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public abstract class BaseHttpClientService {

    private HttpClient httpClient;

    public BaseHttpClientService() {
        this.httpClient = new DefaultHttpClient();
    }

    public void shutdown() {
        if (this.httpClient != null) {
            this.httpClient.getConnectionManager().shutdown();
            this.httpClient = null;
        }
    }

    protected HttpClient getHttpClient() {
        if (httpClient == null) {
            this.httpClient = new DefaultHttpClient();
        }
        return httpClient;
    }

}
