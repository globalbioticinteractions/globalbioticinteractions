package org.eol.globi.util;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.eol.globi.util.ResourceServiceCaching.cacheAndOpenStream;

public class ResourceServiceHTTP extends ResourceServiceCaching {
    private final static Logger LOG = LoggerFactory.getLogger(ResourceServiceHTTP.class);
    private final InputStreamFactory factory;

    public ResourceServiceHTTP(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        LOG.info("caching of [" + resource + "] started...");
        InputStream cachedRemoteInputStream = getCachedRemoteInputStream(resource, factory);
        LOG.info("caching of [" + resource + "] complete.");
        return cachedRemoteInputStream;
    }

    private static InputStream getCachedRemoteInputStream(URI resourceURI, InputStreamFactory factory) throws IOException {
        HttpGet request = new HttpGet(resourceURI);
        try {
            HttpResponse response = HttpUtil.getHttpClient().execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            try (InputStream content = response.getEntity().getContent()) {
                return cacheAndOpenStream(content, factory);
            }
        } finally {
            request.releaseConnection();
        }

    }

}
