package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.net.URI;

public class ResourceServiceFactoryRemote implements ResourceServiceFactory {

    private final InputStreamFactory factory;

    public ResourceServiceFactoryRemote(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public ResourceService serviceForResource(URI resource) {
        ResourceService service = null;
        if (isHttpURI(resource)) {
            service = new ResourceServiceHTTP(factory);
        } else if (StringUtils.startsWith(resource.getScheme(), "ftp")) {
            service = new ResourceServiceFTP(factory);
        }
        return service;
    }

    private boolean isHttpURI(URI descriptor) {
        return "http".equalsIgnoreCase(descriptor.getScheme())
                || "https".equalsIgnoreCase(descriptor.getScheme());
    }

}
