package org.globalbioticinteractions.dataset;

public interface DatasetRegistry {
    Iterable<String> findNamespaces() throws DatasetRegistryException;

    Dataset datasetFor(String namespace) throws DatasetRegistryException;
}
