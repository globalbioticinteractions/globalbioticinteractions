package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetConstant;

import java.io.IOException;
import java.net.URI;

public final class DatasetUtil {

    public static URI getNamedResourceURI(Dataset dataset, URI resourceName) throws IOException {
        return mapResourceNameIfRequested(resourceName, dataset.getConfig());
    }

    public static String getValueOrDefault(JsonNode config, String key, String defaultValue) {
        return config == null
                ? defaultValue
                : (config.has(key) ? config.get(key).asText() : defaultValue);
    }

    public static boolean shouldResolveReferences(Dataset dataset) {
        return dataset == null
        || StringUtils.equalsIgnoreCase("true", dataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"));
    }

    public static URI mapResourceNameIfRequested(URI resourceName, JsonNode config) {
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
