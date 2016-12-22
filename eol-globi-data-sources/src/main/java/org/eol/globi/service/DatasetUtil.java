package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.InputStream;

public final class DatasetUtil {

    public static String getResourceURI(Dataset dataset, String resourceName) {
        String resourceValue = getResource(dataset, resourceName);
        return resourceValue == null ? null : dataset.getResourceURI(resourceValue).toString();
    }

    public static InputStream getResourceStream(Dataset dataset, String resourceName) throws IOException {
        String resourceValue = getResource(dataset, resourceName);
        if (StringUtils.isBlank(resourceValue)) {
            throw new IOException("no resource found for [" + resourceName + "] in [" + dataset.getNamespace() + "]");
        }
        return dataset.getResource(resourceValue);
    }

    private static String getResource(Dataset dataset, String resourceName) {
        String resourceValue = null;
        if (dataset != null) {
            JsonNode config = dataset.getConfig();
            if (config != null && config.has("resources")) {
                JsonNode resources = config.get("resources");
                if (resources.has(resourceName)) {
                    resourceValue = resources.get(resourceName).asText();
                }
            }
        }
        return resourceValue;
    }
}
