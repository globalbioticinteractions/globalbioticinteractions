package org.globalbioticinteractions.dataset;

import java.util.function.Consumer;

public interface DatasetRegistry {
    Iterable<String> findNamespaces() throws DatasetRegistryException;

    void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException;

    Dataset datasetFor(String namespace) throws DatasetRegistryException;
}
