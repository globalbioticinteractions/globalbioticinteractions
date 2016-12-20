package org.eol.globi.service;

import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class Dataset {

    private String namespace;
    private URI archiveURI;

    public Dataset(String namespace, URI archiveURI) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
    }

    public InputStream getResource(String resourceName) throws IOException {
        return ResourceUtil.asInputStream(archiveURI + resourceName, Dataset.class);
    }

    public URI getResourceURI(String resourceName) throws IOException {
        return URI.create(archiveURI + resourceName);
    }


    public URI getArchiveURI() {
        return archiveURI;
    }

    public String getNamespace() {
        return namespace;
    }
}
