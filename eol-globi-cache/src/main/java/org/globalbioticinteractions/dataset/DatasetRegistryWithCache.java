package org.globalbioticinteractions.dataset;

import org.globalbioticinteractions.cache.CacheFactory;

import java.util.Collection;

public class DatasetRegistryWithCache implements DatasetRegistry {
    private final DatasetRegistry registry;

    private final CacheFactory cacheFactory;

    public DatasetRegistryWithCache(DatasetRegistry registry, CacheFactory factory) {
        this.registry = registry;
        this.cacheFactory = factory;
    }

    public Collection<String> findNamespaces() throws DatasetRegistryException {
        return getRegistry().findNamespaces();
    }

    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
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