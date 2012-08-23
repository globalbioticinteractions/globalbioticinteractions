package org.trophic.graph.service;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BaseService {
    protected HttpClient httpClient;

    public BaseService() {
        this.httpClient = new DefaultHttpClient();
    }

    public void shutdown() {
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
    }

}
