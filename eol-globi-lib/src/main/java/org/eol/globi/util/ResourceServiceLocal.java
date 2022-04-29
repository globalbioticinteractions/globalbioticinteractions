package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceLocal implements ResourceService {

    private final InputStreamFactory factory;
    private final Class classContext;

    public ResourceServiceLocal() {
        this(is -> is, ResourceServiceLocal.class);
    }

    public ResourceServiceLocal(InputStreamFactory factory) {
        this(factory, ResourceServiceLocal.class);
    }

    public ResourceServiceLocal(InputStreamFactory factory, Class classContext) {
        this.factory = factory;
        this.classContext = classContext;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return resourceName == null
                ? null
                : new ResourceServiceFactoryLocal(factory, classContext)
                .serviceForResource(resourceName)
                .retrieve(resourceName);
    }
}
