package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpUtil {
    public static HttpClient createHttpClient() {
        return new DefaultHttpClient();
    }
}
