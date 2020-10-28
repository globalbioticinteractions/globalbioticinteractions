package org.globalbioticinteractions.dataset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

interface DatasetConfigurer {
    JsonNode configure(ResourceService dataset, URI configURI) throws IOException;
}

interface DatasetFactoryInterface {
    Dataset datasetFor(String datasetURI) throws DatasetRegistryException;
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
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        Dataset dataset = registry.datasetFor(namespace);
        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        try {
            Pair<URI, JsonNode> jsonNode = configDataset(dataset);
            datasetProxy.setConfig(jsonNode.getRight());
            datasetProxy.setConfigURI(jsonNode.getLeft());
        } catch (Throwable ex) {
            String msg = "failed to configure dataset in namespace [" + namespace + "]";
            throw new DatasetRegistryException(dataset == null ? msg : (msg + " with archiveURI [" + dataset.getArchiveURI() + "] and citation [" + dataset.getCitation() + "]"), ex);
        }
        return datasetProxy;
    }

    private Pair<URI, JsonNode> configDataset(ResourceService dataset) throws DatasetRegistryException {
        Map<URI, DatasetConfigurer> datasetHandlers = new TreeMap<URI, DatasetConfigurer>() {{
            put(URI.create("/globi.json"), new JSONConfigurer());
            put(URI.create("/globi-dataset.jsonld"), new JSONConfigurer());
            put(URI.create("/eml.xml"), (dataset1, uri) -> EMLUtil.datasetWithEML(dataset, uri));
        }};

        Pair<URI, JsonNode> configPair = null;
        for (URI configResource : datasetHandlers.keySet()) {
            try {
                DatasetConfigurer right = datasetHandlers.get(configResource);
                JsonNode config = right.configure(dataset, configResource);
                if (config != null) {
                    configPair = Pair.of(configResource, config);
                    break;
                }
            } catch (IOException e) {
                //
            }
        }
        if (configPair == null) {
            throw new DatasetRegistryException("failed to valid find dataset configuration in [" + StringUtils.join(datasetHandlers.keySet() + "]"));
        }

        return configPair;
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }

    private static class JSONConfigurer implements DatasetConfigurer {

        @Override
        public JsonNode configure(ResourceService dataset, URI configURI) throws IOException {
            return configureDataset(dataset, configURI);
        }
    }

    private static JsonNode configureDataset(ResourceService dataset, URI configURI) throws IOException {
        try (InputStream inputStream = dataset.retrieve(configURI)) {
            if (inputStream == null) {
                throw new IOException("failed to access resource [" + configURI.toString() + "]");
            }
            String descriptor = getContent(configURI, inputStream);
            if (StringUtils.isBlank(descriptor)) {
                throw new IOException("no content found at [" + configURI.toString() + "]");
            }
            return new ObjectMapper().readTree(descriptor);
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