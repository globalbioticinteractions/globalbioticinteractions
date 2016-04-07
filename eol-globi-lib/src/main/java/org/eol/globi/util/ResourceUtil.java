package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.util.zip.GZIPInputStream;

public class ResourceUtil {

    public static final String SHAPEFILES_DIR = "shapefiles.dir";
    private static final Log LOG = LogFactory.getLog(ResourceUtil.class);

    public static InputStream asInputStream(String resource, Class clazz) throws IOException {
        InputStream is = null;
        if (StringUtils.startsWith(resource, "http://")
                || StringUtils.startsWith(resource, "https://")) {
            LOG.info("caching of [" + resource + "] started...");
            is = getCachedRemoteInputStream(resource);
            LOG.info("caching of [" + resource + "] complete.");
        } else if (StringUtils.startsWith(resource, "file://")) {
            is = new FileInputStream(new File(URI.create(resource)));
        } else if (clazz != null) {
            is = clazz.getResourceAsStream(resource);
        }
        if (is == null) {
            URI uri;
            if (StringUtils.startsWith(resource, "/")) {
                uri = new File(resource).toURI();
            } else {
                uri = fromShapefileDir(resource);
            }
            if (uri == null) {
                throw new IOException("failed to open resource [" + resource + "]");
            } else {
                is = new FileInputStream(new File(uri));
            }
        }
        if (StringUtils.endsWith(resource, ".gz")) {
            is = new GZIPInputStream(is);
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
            return cacheAndOpenStream(response.getEntity().getContent());
        } finally {
            request.releaseConnection();
        }

    }

    public static InputStream cacheAndOpenStream(InputStream is) throws IOException {
        File tempFile = File.createTempFile("globiRemote", "tmp");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        IOUtils.copy(is, fos);
        fos.flush();
        IOUtils.closeQuietly(fos);
        return new FileInputStream(tempFile);
    }

    public static URI fromShapefileDir(String shapeFile) {
        URI resourceURI = null;
        String shapeFileDir = System.getProperty(SHAPEFILES_DIR);
        if (StringUtils.isNotBlank(shapeFileDir)) {
            File file = new File(shapeFileDir + shapeFile);
            resourceURI = file.toURI();
        }
        return resourceURI;
    }
}
