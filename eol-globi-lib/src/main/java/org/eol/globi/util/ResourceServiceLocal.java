package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceLocal implements ResourceService {
    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return resourceName == null
                ? null
                : ResourceUtil.asInputStream(resourceName.toString());
    }
}
