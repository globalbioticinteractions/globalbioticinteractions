package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;

public abstract class DatasetMapped implements Dataset {

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
