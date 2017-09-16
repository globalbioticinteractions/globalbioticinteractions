package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetImpl extends DatasetMapped {

    private String namespace;
    private URI archiveURI;
    private JsonNode config;
    private URI configURI;

    public DatasetImpl(String namespace, URI archiveURI) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
    }

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        String mappedResource = mapResourceNameIfRequested(resourceName, getConfig());
        return ResourceUtil.asInputStream(getResourceURI(mappedResource).toString());
    }

    @Override
    public URI getResourceURI(String resourceName) {
        String mappedResource = mapResourceNameIfRequested(resourceName, getConfig());
        return ResourceUtil.getAbsoluteResourceURI(getArchiveURI(), mappedResource);
    }

    @Override
    public URI getArchiveURI() {
        return archiveURI;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setConfig(JsonNode node) {
        this.config = node;
    }

    @Override
    public JsonNode getConfig() {
        return config;
    }

    @Override
    public String getCitation() {
        return CitationUtil.citationFor(this);
    }

    @Override
    public String getFormat() {
        return getOrDefault("format", "globi");
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return DatasetUtil.getValueOrDefault(getConfig(), key, defaultValue);
    }

    @Override
    public String getDOI() {
        return getOrDefault("doi", "");
    }

    public void setConfigURI(URI configURI) {
        this.configURI = configURI;
    }

    @Override
    public URI getConfigURI() {
        return configURI;
    }
}
