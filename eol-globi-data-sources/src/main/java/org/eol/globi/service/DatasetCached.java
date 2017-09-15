package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetCached implements Dataset {

    private final URI archiveCacheURI;
    private final Dataset dataset;

    public DatasetCached(Dataset dataset, URI archiveCacheURI) {
        this.dataset = dataset;
        this.archiveCacheURI = archiveCacheURI;
    }

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        return ResourceUtil.asInputStream(resourceName, this);
    }

    @Override
    public URI getResourceURI(String resourceName) {
        return ResourceUtil.getResourceURI(resourceName, this, archiveCacheURI);
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
