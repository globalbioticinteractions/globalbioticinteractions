package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class ResourceServiceLocalJarResource implements ResourceService {

    private final InputStreamFactory factory;

    public ResourceServiceLocalJarResource(InputStreamFactory factory) {
        this.factory = factory;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        URL url = resourceName.toURL();
        URLConnection urlConnection = url.openConnection();
        // Prevent leaking of jar file descriptors by disabling jar cache.
        // see https://stackoverflow.com/a/36518430
        urlConnection.setUseCaches(false);
        return factory.create(urlConnection.getInputStream());
    }
}
