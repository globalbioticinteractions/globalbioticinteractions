package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceDataDir implements ResourceService {
    private static final String DATA_DIR = "shapefiles.dir";

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        final URI uri = fromDataDir(resourceName);
        if (uri == null) {
            throw new IOException("failed to open resource [" + resourceName + "]");
        } else {
            return new FileInputStream(new File(uri));
        }
    }

    private URI fromDataDir(URI shapeFile) {
        URI resourceURI = null;
        String shapeFileDir = System.getProperty(DATA_DIR);
        if (StringUtils.isNotBlank(shapeFileDir)) {
            File file = new File(shapeFileDir + shapeFile);
            resourceURI = file.toURI();
        }
        return resourceURI;
    }


}
