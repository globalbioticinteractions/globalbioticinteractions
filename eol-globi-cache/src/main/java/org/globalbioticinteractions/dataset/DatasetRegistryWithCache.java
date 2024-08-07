package org.globalbioticinteractions.dataset;

import org.globalbioticinteractions.cache.CacheFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class DatasetRegistryWithCache implements DatasetRegistry {
    private final DatasetRegistry registry;

    private final CacheFactory cacheFactory;

    public DatasetRegistryWithCache(DatasetRegistry registry, CacheFactory factory) {
        this.registry = registry;
        this.cacheFactory = factory;
    }

    public Iterable<String> findNamespaces() throws DatasetRegistryException {
        return getRegistry().findNamespaces();
    }

    @Override
    public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
        for (String namespace : getRegistry().findNamespaces()) {
            namespaceConsumer.accept(namespace);
        }
    }

    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        Dataset dataset = getRegistry().datasetFor(namespace);
        try {
            return new DatasetWithCache(dataset, getCacheFactory().cacheFor(dataset));
        } catch (IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

    private DatasetRegistry getRegistry() {
        return this.registry;
    }

    private CacheFactory getCacheFactory() {
        return cacheFactory;
    }


}