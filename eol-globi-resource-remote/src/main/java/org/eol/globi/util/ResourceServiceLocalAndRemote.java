package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceLocalAndRemote implements ResourceService {

    private final InputStreamFactory factory;
    private final File tmpDir;

    public ResourceServiceLocalAndRemote(InputStreamFactory factory, File tmpDir) {
        this.factory = factory;
        this.tmpDir = tmpDir;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        InputStream is;
        ResourceService resourceService = new ResourceServiceFactoryRemote(factory, tmpDir)
                .serviceForResource(resource);

        if (resourceService != null) {
            is = resourceService.retrieve(resource);
        } else {
            resourceService = new ResourceServiceFactoryLocal(factory)
                    .serviceForResource(resource);
            is = resourceService.retrieve(resource);
        }

        if (is == null) {
            throw new IOException("failed to open resource release to [" + resource.toString() + "]");
        }

        return is;
    }
}
