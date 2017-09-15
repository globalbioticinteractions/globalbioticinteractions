package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.BlobStore;
import org.eol.globi.util.BlobStoreTmpCache;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetCached extends DatasetStored {

    private final URI archiveCacheURI;
    private final Dataset dataset;
    private final BlobStore blobStore;

    public DatasetCached(Dataset dataset, URI archiveCacheURI) {
        this(dataset, archiveCacheURI, new BlobStoreTmpCache());
    }

    public DatasetCached(Dataset dataset, URI archiveCacheURI, BlobStore blobStore) {
        this.dataset = dataset;
        this.archiveCacheURI = archiveCacheURI;
        this.blobStore = blobStore;
    }

    @Override
    BlobStore getBlobStore() {
        return blobStore;
    }

    @Override
    public URI getResourceURI(String resourceName) {
        String mappedResource = mapResourceNameIfRequested(resourceName, getConfig());
        return blobStore.getAbsoluteResourceURI(archiveCacheURI, mappedResource);
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return dataset.getOrDefault(key, defaultValue);
    }


    public URI getArchiveURI() {
        return dataset.getArchiveURI();
    }

    public String getNamespace() {
        return dataset.getNamespace();
    }

    public JsonNode getConfig() {
        return dataset.getConfig();
    }

    public String getCitation() {
        return dataset.getCitation();
    }

    public String getFormat() {
        return dataset.getFormat();
    }


    public String getDOI() {
        return dataset.getDOI();
    }

    public URI getConfigURI() {
        return dataset.getConfigURI();
    }

    @Override
    public void setConfig(JsonNode config) {
        dataset.setConfig(config);
    }

    @Override
    public void setConfigURI(URI configURI) {
        dataset.setConfigURI(configURI);
    }

}
