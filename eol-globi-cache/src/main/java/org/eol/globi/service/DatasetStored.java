package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.ResourceCache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public abstract class DatasetStored implements Dataset {

    abstract ResourceCache getResourceCache();

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        String mappedResource = mapResourceNameIfRequested(resourceName, getConfig());
        return getResourceCache().asInputStream(getResourceURI(mappedResource).toString());
    }

    @Override
    public URI getResourceURI(String resourceName) {
        String mappedResource = mapResourceNameIfRequested(resourceName, getConfig());
        return getResourceCache().getAbsoluteResourceURI(getArchiveURI(), mappedResource);
    }

    protected String mapResourceNameIfRequested(String resourceName, JsonNode config) {
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

}
