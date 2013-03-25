package org.eol.globi.service;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public abstract class BaseHttpClientService {

    protected HttpClient httpClient;

    public BaseHttpClientService() {
        this.httpClient = new DefaultHttpClient();
    }

    public void shutdown() {
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
    }

}
