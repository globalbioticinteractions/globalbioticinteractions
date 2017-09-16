package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceCache;
import org.eol.globi.util.ResourceCacheTmp;

import java.net.URI;

public class DatasetCached extends DatasetStored {

    private final URI archiveCacheURI;
    private final Dataset dataset;
    private final ResourceCache resourceCache;

    public DatasetCached(Dataset dataset, URI archiveCacheURI) {
        this(dataset, archiveCacheURI, new ResourceCacheTmp());
    }

    public DatasetCached(Dataset dataset, URI archiveCacheURI, ResourceCache resourceCache) {
        this.dataset = dataset;
        this.archiveCacheURI = archiveCacheURI;
        this.resourceCache = resourceCache;
    }

    @Override
    ResourceCache getResourceCache() {
        return resourceCache;
    }

    @Override
    public URI getResourceURI(String resourceName) {
        String mappedResource = mapResourceNameIfRequested(resourceName, getConfig());
        return resourceCache.getAbsoluteResourceURI(archiveCacheURI, mappedResource);
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
