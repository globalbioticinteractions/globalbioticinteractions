package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.BlobStore;
import org.eol.globi.util.BlobStoreTmpCache;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class DatasetFactory {

    public static Dataset datasetFor(String repo, DatasetFinder finder) throws DatasetFinderException {
        return configDataset(finder.datasetFor(repo), new BlobStoreTmpCache());
    }

    private static Dataset configDataset(Dataset dataset, BlobStore blobStore) throws DatasetFinderException {
        List<String> configResources = Arrays.asList("/globi.json", "/globi-dataset.jsonld");

        URI configURI = null;
        for (String configResource : configResources) {
            configURI = dataset.getResourceURI(configResource);
            if (blobStore.resourceExists(configURI)) {
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
        String descriptor = getContent(configURI, dataset.getResource(configURI.toString()));
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            dataset.setConfigURI(configURI);
            dataset.setConfig(desc);
        }
        return dataset;
    }

    private static String getContent(URI uri, InputStream input) throws IOException {
        try {
            return IOUtils.toString(input);
        } catch (IOException ex) {
            throw new IOException("failed to find [" + uri + "]", ex);
        }
    }


}