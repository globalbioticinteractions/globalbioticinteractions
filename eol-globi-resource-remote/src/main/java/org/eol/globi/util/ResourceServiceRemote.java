package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceRemote implements ResourceService {

    private final InputStreamFactory factory;
    private final File cacheDir;

    public ResourceServiceRemote(InputStreamFactory factory, File cacheDir) {
        this.factory = factory;
        this.cacheDir = cacheDir;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        InputStream is = null;

        if (resourceName != null) {
            ResourceService resourceService = new ResourceServiceFactoryRemote(factory, cacheDir)
                    .serviceForResource(resourceName);
            if (resourceService == null) {
                throw new IOException("cannot retrieve content of unsupported resource identifier [" + resourceName.toString() + "]");
            } else {
                is = resourceService.retrieve(resourceName);
            }
        }

        return is;
    }
}
