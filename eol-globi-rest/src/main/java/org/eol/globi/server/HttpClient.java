package org.eol.globi.server;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class HttpClient {
    protected static final String APPLICATION_JSON = "application/json";

    public static String httpGet(String uri) throws IOException {
        org.apache.http.client.HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri);
        addJsonHeaders(httpGet);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return httpclient.execute(httpGet, responseHandler);
    }

    public static void addJsonHeaders(HttpRequestBase httpGet) {
        httpGet.setHeader("Accept", APPLICATION_JSON);
        httpGet.setHeader("Content-Type", APPLICATION_JSON);
    }
}
