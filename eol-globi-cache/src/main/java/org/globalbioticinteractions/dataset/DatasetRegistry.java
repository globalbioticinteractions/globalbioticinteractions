package org.globalbioticinteractions.dataset;

import java.util.Collection;

public interface DatasetRegistry {
    Collection<String> findNamespaces() throws DatasetRegistryException;

    Dataset datasetFor(String namespace) throws DatasetRegistryException;
}
