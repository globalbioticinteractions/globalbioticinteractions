package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.File;
import java.net.URI;

public class ResourceServiceFactoryRemote implements ResourceServiceFactory {

    private final InputStreamFactory factory;
    private final File tmpDir;

    public ResourceServiceFactoryRemote(InputStreamFactory factory, File tmpDir) {
        this.factory = factory;
        this.tmpDir = tmpDir;
    }

    @Override
    public ResourceService serviceForResource(URI resource) {
        ResourceService service = null;
        if (isHttpURI(resource)) {
            service = new ResourceServiceHTTP(factory, tmpDir);
        } else if (StringUtils.startsWith(resource.getScheme(), "ftp")) {
            service = new ResourceServiceFTP(factory, tmpDir);
        }
        return service == null
                ? null
                : new ResourceServiceGzipAware(service);
    }

    private boolean isHttpURI(URI descriptor) {
        return "http".equalsIgnoreCase(descriptor.getScheme())
                || "https".equalsIgnoreCase(descriptor.getScheme());
    }

}
