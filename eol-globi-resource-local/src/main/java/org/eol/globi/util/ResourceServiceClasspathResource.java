package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceClasspathResource implements ResourceService {

    private final InputStreamFactory factory;
    private final Class clazz;

    public ResourceServiceClasspathResource(InputStreamFactory factory) {
        this(factory, ResourceServiceClasspathResource.class);
    }

    public ResourceServiceClasspathResource(InputStreamFactory factory, Class clazz) {
        this.factory = factory;
        this.clazz = clazz;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        String classpathResource = resourceName.toString();
        if (StringUtils.startsWith(classpathResource, "classpath:")) {
            classpathResource = StringUtils.replace(classpathResource, "classpath:", "");
        }
        InputStream is = factory.create(clazz.getResourceAsStream(classpathResource));
        if (is == null) {
            is = new ResourceServiceDataDir().retrieve(resourceName);
        }

        if (is == null) {
            throw new IOException("resource [" + resourceName.toString() + "] not found");
        }
        return is;
    }
}
