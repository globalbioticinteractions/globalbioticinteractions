package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.net.URI;

public abstract class DatasetMapped implements Dataset {

    protected URI mapResourceNameIfRequested(URI resourceName, JsonNode config) {
        URI mappedResource = resourceName;
        if (config != null && config.has("resources")) {
            JsonNode resources = config.get("resources");
            if (resources.isObject() && resources.has(resourceName.toString())) {
                JsonNode resourceName1 = resources.get(resourceName.toString());
                if (resourceName1.isTextual()) {
                    String resourceNameCandidate = resourceName1.asText();
                    mappedResource = StringUtils.isBlank(resourceNameCandidate)
                            ? mappedResource
                            : URI.create(resourceNameCandidate);
                }
            }
        }
        return mappedResource;
    }

}
