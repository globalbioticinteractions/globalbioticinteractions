package org.globalbioticinteractions.dataset;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.InputStreamFactoryNoop;

import java.io.IOException;
import java.net.URI;
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
            put(URI.create("/metadata.yaml"), (dataset1, uri) -> CatalogueOfLifeDataPackageUtil.datasetFor(resourceService, uri));
            put(URI.create("/globi.json"), new JSONConfigurer());
            put(URI.create("/globi-dataset.jsonld"), new JSONConfigurer());
            put(URI.create("/meta.xml"), new JSONConfigurer());
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
            String msg = "failed to find valid dataset configuration in [" + StringUtils.join(datasetHandlers.keySet() + "]");
            throw lastThrown == null
                    ? new DatasetRegistryException(msg)
                    : new DatasetRegistryException(msg, lastThrown);
        }

        return configPair;
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }


}