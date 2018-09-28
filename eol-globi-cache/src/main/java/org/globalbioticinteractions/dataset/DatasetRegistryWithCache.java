package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetRegistry;
import org.eol.globi.service.DatasetFinderException;
import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.CacheUtil;

import java.util.Collection;

public class DatasetRegistryWithCache implements DatasetRegistry {
    private final DatasetRegistry finder;

    private final CacheFactory cacheFactory;

    public DatasetRegistryWithCache(DatasetRegistry finder) {
        this(finder, "target/datasets");
    }

    public DatasetRegistryWithCache(DatasetRegistry finder, String cachePath) {
        this(finder, dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath));
    }

    public DatasetRegistryWithCache(DatasetRegistry finder, CacheFactory factory) {
        this.finder = finder;
        this.cacheFactory = factory;
    }

    public Collection<String> findNamespaces() throws DatasetFinderException {
        return getFinder().findNamespaces();
    }

    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        Dataset dataset = getFinder().datasetFor(namespace);
        return new DatasetWithCache(dataset, getCacheFactory().cacheFor(dataset));
    }

    DatasetRegistry getFinder() {
        return this.finder;
    }

    CacheFactory getCacheFactory() {
        return cacheFactory;
    }


}