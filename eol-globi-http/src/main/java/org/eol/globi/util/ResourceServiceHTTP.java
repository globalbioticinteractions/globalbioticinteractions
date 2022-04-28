package org.eol.globi.util;

import org.eol.globi.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceHTTP implements ResourceService {
    private final static Logger LOG = LoggerFactory.getLogger(ResourceServiceHTTP.class);
    private final InputStreamFactory factory;

    public ResourceServiceHTTP(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        LOG.info("caching of [" + resource + "] started...");
        InputStream cachedRemoteInputStream = ResourceUtil.getCachedRemoteInputStream(resource, factory);
        LOG.info("caching of [" + resource + "] complete.");
        return cachedRemoteInputStream;
    }
}
