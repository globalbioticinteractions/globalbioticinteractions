package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.ReferenceUtil;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.join;

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
        String mappedResource = mapResourceNameIfRequested(resourceName);
        return ResourceUtil.asInputStream(getResourceURI(mappedResource), DatasetImpl.class);
    }

    public String mapResourceNameIfRequested(String resourceName) {
        String mappedResource = resourceName;
        if (getConfig() != null && getConfig().has("resources")) {
            JsonNode resources = getConfig().get("resources");
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
        String mappedResourceName = mapResourceNameIfRequested(resourceName);
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
