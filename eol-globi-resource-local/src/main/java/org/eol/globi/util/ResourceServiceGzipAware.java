package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

public class ResourceServiceGzipAware implements ResourceService {
    private final ResourceService resourceService;

    public ResourceServiceGzipAware(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        InputStream is = resourceService.retrieve(resourceName);
        if (StringUtils.endsWith(resourceName.toString(), ".gz")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }
}
