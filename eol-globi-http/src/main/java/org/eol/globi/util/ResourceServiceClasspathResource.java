package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceClasspathResource implements ResourceService {

    private final InputStreamFactory factory;

    public ResourceServiceClasspathResource(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        String classpathResource = resourceName.toString();
        if (StringUtils.startsWith(classpathResource, "classpath:")) {
            classpathResource = StringUtils.replace(classpathResource, "classpath:", "");
        }
        InputStream is = factory.create(ResourceUtil.class.getResourceAsStream(classpathResource));
        if (is == null) {
            is = new ResourceServiceDataDir().retrieve(resourceName);
        }

        if (is == null) {
            throw new IOException("resource [" + resourceName.toString() + "] not found");
        }
        return is;
    }
}
