package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetRemote implements Dataset {

    private String namespace;
    private URI archiveURI;
    private JsonNode config;
    private URI configURI;

    public DatasetRemote(String namespace, URI archiveURI) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
    }

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        return ResourceUtil.asInputStream(getResourceURI(resourceName), DatasetRemote.class);
    }

    @Override
    public URI getResourceURI(String resourceName) {
        return ResourceUtil.getAbsoluteResourceURI(getArchiveURI(), resourceName);
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
        return getOrDefault("citation", getArchiveURI().toString());
    }

    @Override
    public String getFormat() {
        return getOrDefault("format", "globi");
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return getConfig() == null
                ? defaultValue
                : (getConfig().has(key) ? getConfig().get(key).asText() : defaultValue);
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
