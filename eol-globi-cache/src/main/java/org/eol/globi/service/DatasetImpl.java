package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceCache;
import org.eol.globi.util.ResourceCacheTmp;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.net.URI;

public class DatasetImpl extends DatasetStored {

    private String namespace;
    private URI archiveURI;
    private JsonNode config;
    private URI configURI;
    private ResourceCache resourceCache;

    public DatasetImpl(String namespace, URI archiveURI) {
        this(namespace, archiveURI, new ResourceCacheTmp());
    }

    public DatasetImpl(String namespace, URI archiveURI, ResourceCache store) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
        this.resourceCache = store;
    }

    @Override
    ResourceCache getResourceCache() {
        return resourceCache;
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
