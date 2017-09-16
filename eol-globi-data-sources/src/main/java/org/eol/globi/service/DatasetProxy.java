package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetProxy implements Dataset {

    private JsonNode config;
    private final Dataset datasetProxied;

    public DatasetProxy(Dataset datasetProxied) {
        this.datasetProxied = datasetProxied;
    }

    @Override
    public InputStream getResource(String resourcePath) throws IOException {
        return datasetProxied.getResource(resourcePath);
    }

    @Override
    public URI getResourceURI(String resourcePath) {
        return datasetProxied.getResourceURI(resourcePath);
    }

    @Override
    public URI getArchiveURI() {
        return datasetProxied.getArchiveURI();
    }

    @Override
    public String getNamespace() {
        return datasetProxied.getNamespace();
    }

    @Override
    public JsonNode getConfig() {
        return (config == null) ? datasetProxied.getConfig() : config;
    }

    @Override
    public String getCitation() {
        return CitationUtil.citationOrDefaultFor(this, datasetProxied.getCitation());
    }

    @Override
    public String getFormat() {
        return DatasetUtil.getValueOrDefault(config, "format", datasetProxied.getFormat());
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return (config != null && config.has(key))
                ? config.get(key).asText()
                : datasetProxied.getOrDefault(key, defaultValue);
    }

    @Override
    public String getDOI() {
        return DatasetUtil.getValueOrDefault(config, "doi", datasetProxied.getFormat());
    }

    @Override
    public URI getConfigURI() {
        return datasetProxied.getConfigURI();
    }

    @Override
    public void setConfig(JsonNode config) {
        this.config = config;
    }

    @Override
    public void setConfigURI(URI configURI) {
        datasetProxied.setConfigURI(configURI);
    }
}
