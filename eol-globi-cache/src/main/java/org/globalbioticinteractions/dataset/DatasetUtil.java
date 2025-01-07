package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.net.URI;

import static org.globalbioticinteractions.dataset.DatasetConstant.DEPRECATED;

public final class DatasetUtil {

    public static URI getNamedResourceURI(Dataset dataset, URI resourceName) throws IOException {
        URI mappedResource = mapResourceNameIfRequested(resourceName, dataset.getConfig(), "resources");
        URI versionedResource = mapResourceNameIfRequested(mappedResource, dataset.getConfig(), "versions");
        return versionedResource;
    }

    public static String getValueOrDefault(JsonNode config, String key, String defaultValue) {
        return config == null
                ? defaultValue
                : (config.has(key) && config.get(key).isTextual() ? config.get(key).asText() : defaultValue);
    }

    public static boolean shouldResolveReferences(Dataset dataset) {
        return dataset == null
        || StringUtils.equalsIgnoreCase("true", dataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"));
    }

    public static URI mapResourceNameIfRequested(URI resourceName, JsonNode config, String resourceType) {
        URI mappedResource = resourceName;
        if (config != null && config.has(resourceType)) {
            JsonNode resources = config.get(resourceType);
            if (resources != null && resources.isObject() && resources.has(resourceName.toString())) {
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

    public static URI mapResourceForDataset(Dataset dataset, URI resourceName) throws IOException {
        URI mappedResource = getNamedResourceURI(dataset, resourceName);
        return ResourceUtil.getAbsoluteResourceURI(dataset.getArchiveURI(), mappedResource);
    }

    public static boolean isDeprecated(Dataset dataset) {
        return dataset == null
                || StringUtils.equalsIgnoreCase("true", dataset.getOrDefault(DEPRECATED, "false"));
    }
}
