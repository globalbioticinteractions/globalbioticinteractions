package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

public class ResourceServiceLocalAndRemote implements ResourceService {

    private final InputStreamFactory factory;

    public ResourceServiceLocalAndRemote(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        InputStream is;
        ResourceService resourceService = new ResourceServiceFactoryRemote(factory)
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
