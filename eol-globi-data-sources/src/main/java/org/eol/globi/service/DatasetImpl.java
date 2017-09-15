package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.ReferenceUtil;
import org.eol.globi.util.BlobStore;
import org.eol.globi.util.BlobStoreTmpCache;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetImpl extends DatasetStored {

    private String namespace;
    private URI archiveURI;
    private JsonNode config;
    private URI configURI;
    private BlobStore blobStore;

    public DatasetImpl(String namespace, URI archiveURI) {
        this(namespace, archiveURI, new BlobStoreTmpCache());
    }

    public DatasetImpl(String namespace, URI archiveURI, BlobStore store) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
        this.blobStore = store;
    }

    @Override
    BlobStore getBlobStore() {
        return blobStore;
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
        return ReferenceUtil.citationFor(this);
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
