package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.net.URI;

public class ResourceServiceFactoryLocal implements ResourceServiceFactory {

    private final InputStreamFactory factory;
    private final Class classContext;

    public ResourceServiceFactoryLocal(InputStreamFactory factory) {
        this(factory, ResourceServiceFactoryLocal.class);
    }

    public ResourceServiceFactoryLocal(InputStreamFactory factory, Class classContext) {
        this.factory = factory;
        this.classContext = classContext;
    }

    @Override
    public ResourceService serviceForResource(URI resource) {
        ResourceService resourceService = null;
        if (ResourceUtil.isFileURI(resource)) {
            resourceService = new ResourceServiceLocalFile(factory);
        } else if (StringUtils.startsWith(resource.toString(), "jar:file:/")) {
            resourceService = new ResourceServiceLocalJarResource(factory);
        } else {
            resourceService = new ResourceServiceClasspathResource(factory, classContext);
        }

        return resourceService;
    }
}
