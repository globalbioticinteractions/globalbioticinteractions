package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceLocal implements ResourceService {

    private ResourceServiceFactoryLocal resourceServiceFactoryLocal;

    public ResourceServiceLocal() {
        this(new InputStreamFactoryNoop());
    }

    public ResourceServiceLocal(InputStreamFactory factory) {
        this(factory, ResourceServiceLocal.class);
    }

    public ResourceServiceLocal(InputStreamFactory factory, Class classContext) {
        this(factory, classContext, null);
    }

    public ResourceServiceLocal(InputStreamFactory factory, Class classContext, String dataDir) {
        resourceServiceFactoryLocal = new ResourceServiceFactoryLocal(factory, classContext, dataDir);
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return resourceName == null
                ? null
                : resourceServiceFactoryLocal.serviceForResource(resourceName).retrieve(resourceName);
    }
}
