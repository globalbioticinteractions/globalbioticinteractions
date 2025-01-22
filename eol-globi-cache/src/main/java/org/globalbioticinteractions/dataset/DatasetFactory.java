package org.globalbioticinteractions.dataset;

interface DatasetFactory {
    Dataset datasetFor(String namespace) throws DatasetRegistryException;
}
