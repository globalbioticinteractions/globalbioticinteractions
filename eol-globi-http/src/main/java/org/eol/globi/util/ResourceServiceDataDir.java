package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceDataDir implements ResourceService {

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        final URI uri = ResourceUtil.fromDataDir(resourceName);
        if (uri == null) {
            throw new IOException("failed to open resource [" + resourceName + "]");
        } else {
            return new FileInputStream(new File(uri));
        }

    }
}
