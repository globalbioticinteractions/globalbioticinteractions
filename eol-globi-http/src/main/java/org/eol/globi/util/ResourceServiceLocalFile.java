package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceLocalFile implements ResourceService {

    private final InputStreamFactory factory;

    public ResourceServiceLocalFile(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return factory.create(new FileInputStream(new File(resourceName)));
    }
}
