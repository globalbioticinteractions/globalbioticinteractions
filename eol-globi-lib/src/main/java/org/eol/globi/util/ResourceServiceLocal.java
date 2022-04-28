package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceLocal implements ResourceService {

    private final InputStreamFactory factory;

    public ResourceServiceLocal(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return resourceName == null
                ? null
                : new ResourceServiceFactoryLocal(factory)
                .serviceForResource(resourceName)
                .retrieve(resourceName);
    }
}
