package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.globalbioticinteractions.cache.CacheFactory;

import java.util.Collection;

public class DatasetRegistryWithCache implements DatasetRegistry {
    private final DatasetRegistry registry;

    private final CacheFactory cacheFactory;

    public DatasetRegistryWithCache(DatasetRegistry registry, CacheFactory factory) {
        this.registry = registry;
        this.cacheFactory = factory;
    }

    public Collection<String> findNamespaces() throws DatasetFinderException {
        return getRegistry().findNamespaces();
    }

    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        Dataset dataset = getRegistry().datasetFor(namespace);
        return new DatasetWithCache(dataset, getCacheFactory().cacheFor(dataset));
    }

    private DatasetRegistry getRegistry() {
        return this.registry;
    }

    private CacheFactory getCacheFactory() {
        return cacheFactory;
    }


}