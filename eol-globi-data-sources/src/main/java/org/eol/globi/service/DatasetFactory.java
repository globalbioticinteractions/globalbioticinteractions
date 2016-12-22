package org.eol.globi.service;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class DatasetFactory {

    public static Dataset datasetFor(String repo, DatasetFinder finder) throws DatasetFinderException {
        return configDataset(finder.datasetFor(repo));
    }

    private static Dataset configDataset(Dataset dataset) throws DatasetFinderException {
        List<String> configResources = Arrays.asList("/globi.json", "/globi-dataset.jsonld");

        URI configURI = null;
        for (String configResource : configResources) {
            configURI = dataset.getResourceURI(configResource);
            if (ResourceUtil.resourceExists(configURI)) {
                break;
            }
        }
        try {
            return configureDataset(dataset, configURI);
        } catch (IOException e) {
            throw new DatasetFinderException("failed to import [" + dataset.getNamespace() + "]", e);
        }
    }

    private static Dataset configureDataset(Dataset dataset, URI configURI) throws IOException {
        String descriptor = ResourceUtil.getContent(configURI);
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            dataset.setConfigURI(configURI);
            dataset.setConfig(desc);
        }
        return dataset;
    }


}