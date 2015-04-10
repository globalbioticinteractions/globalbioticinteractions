package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceUtil {
    public static InputStream asInputStream(String resource, Class clazz) throws IOException {
        InputStream is;
        if (StringUtils.startsWith(resource, "http://")
                || StringUtils.startsWith(resource, "https://")) {
            is = getCachedRemoteInputStream(resource);
        } else {
            is = clazz.getResourceAsStream(resource);
            if (is == null) {
                throw new IOException("failed to open study resource [" + resource + "]");
            }
        }
        return is;
    }

    private static InputStream getCachedRemoteInputStream(String resource) throws IOException {
        URI resourceURI = URI.create(resource);
        HttpGet request = new HttpGet(resourceURI);
        try {
            HttpResponse response = HttpUtil.getHttpClient().execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            return openCachedStream(response);
        } finally {
            request.releaseConnection();
        }
    }

    private static InputStream openCachedStream(HttpResponse response) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(response.getEntity().getContent(), fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
        return new FileInputStream(tempFile);
    }
}
