package org.globalbioticinteractions.dataset;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.InputStreamFactoryNoop;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

interface DatasetConfigurer {
    JsonNode configure(ResourceService dataset, URI configURI) throws IOException;
}

public class DatasetFactoryImpl implements DatasetFactory {

    private final DatasetRegistry registry;
    private final InputStreamFactory inputStreamFactory;

    public DatasetFactoryImpl(DatasetRegistry registry) {
        this(registry, new InputStreamFactoryNoop());
    }

    public DatasetFactoryImpl(DatasetRegistry registry, InputStreamFactory factory) {
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
            String msg = "unrecognized dataset format: cannot process dataset in namespace [" + namespace + "]";
            throw new DatasetRegistryException(dataset == null ? msg : (msg + " with archiveURI [" + dataset.getArchiveURI() + "] and citation [" + dataset.getCitation() + "]"), ex);
        }
        return datasetProxy;
    }

    private Pair<URI, JsonNode> configDataset(ResourceService resourceService) throws DatasetRegistryException {
        Map<URI, DatasetConfigurer> datasetHandlers = new LinkedMap<URI, DatasetConfigurer>() {{
            put(URI.create("/eml.xml"), (dataset1, uri) -> EMLUtil.datasetFor(resourceService, uri));
            put(URI.create("/datapackage.json"), (dataset1, uri) -> DwCDataPackageUtil.datasetFor(resourceService, uri));
            put(URI.create("/globi.json"), new JSONConfigurer());
            put(URI.create("/globi-dataset.jsonld"), new JSONConfigurer());
        }};

        Pair<URI, JsonNode> configPair = null;
        IOException lastThrown = null;
        for (URI configResource : datasetHandlers.keySet()) {
            try {
                DatasetConfigurer right = datasetHandlers.get(configResource);
                JsonNode config = right.configure(resourceService, configResource);
                if (config != null) {
                    configPair = Pair.of(configResource, config);
                    break;
                }
            } catch (IOException e) {
                lastThrown = e;
            }
        }
        if (configPair == null) {
            String msg = "failed to valid find dataset configuration in [" + StringUtils.join(datasetHandlers.keySet() + "]");
            throw lastThrown == null
                    ? new DatasetRegistryException(msg)
                    : new DatasetRegistryException(msg, lastThrown);
        }

        return configPair;
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }

    private static class JSONConfigurer implements DatasetConfigurer {

        @Override
        public JsonNode configure(ResourceService resourceService, URI configURI) throws IOException {
            return configureDataset(resourceService, configURI);
        }
    }

    private static JsonNode configureDataset(ResourceService sourceService, URI configURI) throws IOException {
        try (InputStream inputStream = sourceService.retrieve(configURI)) {
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