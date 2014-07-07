package org.eol.globi.util;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;

public class HttpClient {
    protected static final String APPLICATION_JSON = "application/json;charset=UTF-8";

    public static String httpGet(String uri) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        addJsonHeaders(httpGet);
        return HttpUtil.createHttpClient().execute(httpGet, new BasicResponseHandler());
    }

    public static void addJsonHeaders(HttpRequestBase httpGet) {
        httpGet.setHeader("Accept", APPLICATION_JSON);
        httpGet.setHeader("Content-Type", APPLICATION_JSON);
    }
}
