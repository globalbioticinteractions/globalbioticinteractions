package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;

import java.net.URI;

public final class DatasetUtil {

    public static String getDataArchiveURI(Dataset dataset) {
        return getResourceURI(dataset, "archive");
    }

    public static String getResourceURI(Dataset dataset, String resourceName) {
        String resourceURI = null;
        if (dataset != null) {
            JsonNode config = dataset.getConfig();
            if (config != null && config.has("resources")) {
                JsonNode resources = config.get("resources");
                if (resources.has(resourceName)) {
                    URI archive = dataset.getResourceURI(resources.get(resourceName).asText());
                    resourceURI = archive.toString();
                }
            }
        }
        return resourceURI;
    }
}
