package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.EMLUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

interface DatasetConfigurer {
    Dataset configure(Dataset dataset, URI configURI) throws IOException;
}

interface DatasetFactoryInterface {
    Dataset datasetFor(String datasetURI) throws DatasetFinderException;
}

public class DatasetFactory implements DatasetFactoryInterface {

    private final DatasetRegistry registry;
    private final InputStreamFactory inputStreamFactory;

    public DatasetFactory(DatasetRegistry registry) {
        this(registry, inStream -> inStream);
    }

    public DatasetFactory(DatasetRegistry registry, InputStreamFactory factory) {
        this.registry = registry;
        this.inputStreamFactory = factory;
    }

    @Override
    public Dataset datasetFor(String datasetURI) throws DatasetFinderException {
        return configDataset(registry.datasetFor(datasetURI));
    }


    @Deprecated
    public static Dataset datasetFor(String repo, DatasetRegistry finder) throws DatasetFinderException {
        return new DatasetFactory(finder).datasetFor(repo);
    }

    private Dataset configDataset(Dataset dataset) throws DatasetFinderException {
        Map<String, DatasetConfigurer> datasetHandlers = new TreeMap<String, DatasetConfigurer>() {{
            put("/globi.json", new JSONConfigurer());
            put("/globi-dataset.jsonld", new JSONConfigurer());
            put("/eml.xml", (dataset1, uri) -> EMLUtil.datasetWithEML(dataset, uri));
        }};

        Pair<URI, DatasetConfigurer> configPair = null;
        for (String configResource : datasetHandlers.keySet()) {
            try {
                URI configURI = dataset.getResourceURI(configResource);
                if (ResourceUtil.resourceExists(configURI, getInputStreamFactory())) {
                    configPair = Pair.of(configURI, datasetHandlers.get(configResource));
                    break;
                }
            } catch (IOException e) {
                throw new DatasetFinderException("failed to access configURI", e);
            }
        }
        try {
            if (configPair == null) {
                throw new DatasetFinderException("failed to import [" + dataset.getNamespace() + "]: cannot locate resource at [" + StringUtils.join(datasetHandlers.keySet(), " , ") + "]");
            }
            return configPair.getRight().configure(dataset, configPair.getLeft());
        } catch (IOException e) {
            throw new DatasetFinderException("failed to import [" + dataset.getNamespace() + "]", e);
        }
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }

    private static class JSONConfigurer implements DatasetConfigurer {

        @Override
        public Dataset configure(Dataset dataset, URI configURI) throws IOException {
            return configureDataset(dataset, configURI);
        }
    }

    private static Dataset configureDataset(Dataset dataset, URI configURI) throws IOException {
        try (InputStream inputStream = dataset.getResource(configURI.toString())) {
            if (inputStream == null) {
                throw new IOException("failed to access resource [" + configURI.toString() + "]");
            }
            String descriptor = getContent(configURI, inputStream);
            if (StringUtils.isNotBlank(descriptor)) {
                JsonNode desc = new ObjectMapper().readTree(descriptor);
                dataset.setConfigURI(configURI);
                dataset.setConfig(desc);
            }
            return dataset;
        }
    }

    private static String getContent(URI uri, InputStream input) throws IOException {
        try {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IOException("failed to find [" + uri + "]", ex);
        }
    }


}