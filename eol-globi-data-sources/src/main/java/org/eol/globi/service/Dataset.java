package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class Dataset {

    private String namespace;
    private URI archiveURI;
    private JsonNode config;
    private String format;

    public Dataset(String namespace, URI archiveURI) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
    }

    public InputStream getResource(String resourceName) throws IOException {
        return ResourceUtil.asInputStream(archiveURI + resourceName, Dataset.class);
    }

    public URI getResourceURI(String resourceName) {
        return ResourceUtil.isURL(resourceName)
                ? URI.create(resourceName)
                : getURIAddSlashIfNeeded(resourceName);
    }

    private URI getURIAddSlashIfNeeded(String resourceName) {
        if (!StringUtils.startsWith(resourceName, "/") && !StringUtils.endsWith(getArchiveURI().toString(), "/")) {
            return URI.create(archiveURI + "/" + resourceName);
        } else {
            return URI.create(archiveURI + resourceName);
        }
    }


    public URI getArchiveURI() {
        return archiveURI;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setConfig(JsonNode node) {
        this.config = node;
    }

    public JsonNode getConfig() {
        return config;
    }

    public String getCitation() {
        return getOrDefault("citation", getArchiveURI().toString());
    }

    public String getFormat() {
        return getOrDefault("format", "globi");
    }

    public String getOrDefault(String key, String defaultValue) {
        return getConfig() == null
                ? defaultValue
                : (getConfig().has(key) ? getConfig().get(key).asText() : defaultValue);
    }

    public String getDOI() {
        return getOrDefault("doi", "");
    }

}
