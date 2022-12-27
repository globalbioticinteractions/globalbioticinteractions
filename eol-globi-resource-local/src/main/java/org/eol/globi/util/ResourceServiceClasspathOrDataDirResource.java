package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceClasspathOrDataDirResource implements ResourceService {

    private final InputStreamFactory factory;
    private final Class clazz;
    private final String dataDir;

    public ResourceServiceClasspathOrDataDirResource(InputStreamFactory factory, Class clazz, String dataDir) {
        this.factory = factory;
        this.clazz = clazz;
        this.dataDir = dataDir;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        String classpathResource = resourceName.toString();
        if (StringUtils.startsWith(classpathResource, "classpath:")) {
            classpathResource = StringUtils.replace(classpathResource, "classpath:", "");
        }
        InputStream is = factory.create(clazz.getResourceAsStream(classpathResource));
        if (is == null) {
            is = new ResourceServiceDataDir(dataDir).retrieve(URI.create(classpathResource));
        }

        if (is == null) {
            throw new IOException("resource [" + resourceName.toString() + "] not found");
        }
        return is;
    }
}
