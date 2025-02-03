package org.globalbioticinteractions.dataset;

public interface DatasetFactory {
    Dataset datasetFor(String namespace) throws DatasetRegistryException;
}
