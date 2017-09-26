package org.globalbioticinteractions.dataset;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderException;
import org.globalbioticinteractions.cache.CacheFactory;
import org.globalbioticinteractions.cache.CacheUtil;

import java.util.Collection;

public class DatasetFinderWithCache implements DatasetFinder {
    private final DatasetFinder finder;

    private final CacheFactory cacheFactory;

    public DatasetFinderWithCache(DatasetFinder finder) {
        this(finder, "target/datasets");
    }

    public DatasetFinderWithCache(DatasetFinder finder, String cachePath) {
        this(finder, dataset -> CacheUtil.cacheFor(dataset.getNamespace(), cachePath));
    }

    public DatasetFinderWithCache(DatasetFinder finder, CacheFactory factory) {
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

    DatasetFinder getFinder() {
        return this.finder;
    }

    CacheFactory getCacheFactory() {
        return cacheFactory;
    }


}