package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.ReferenceUtil;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetImpl implements Dataset {

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
        return ResourceUtil.asInputStream(getResourceURI(mappedResource), DatasetImpl.class);
    }

    public static String mapResourceNameIfRequested(String resourceName, JsonNode config) {
        String mappedResource = resourceName;
        if (config != null && config.has("resources")) {
            JsonNode resources = config.get("resources");
            if (resources.isObject() && resources.has(resourceName)) {
                JsonNode resourceName1 = resources.get(resourceName);
                if (resourceName1.isTextual()) {
                    String resourceNameCandidate = resourceName1.asText();
                    mappedResource = StringUtils.isBlank(resourceNameCandidate) ? mappedResource : resourceNameCandidate;
                }
            }
        }
        return mappedResource;
    }

    @Override
    public URI getResourceURI(String resourceName) {
        String mappedResourceName = mapResourceNameIfRequested(resourceName, getConfig());
        return ResourceUtil.getAbsoluteResourceURI(getArchiveURI(), mappedResourceName);
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
