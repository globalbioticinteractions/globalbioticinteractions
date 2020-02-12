package org.globalbioticinteractions.dataset;

import java.util.Collection;

public interface DatasetRegistry {
    Collection<String> findNamespaces() throws DatasetFinderException;

    Dataset datasetFor(String namespace) throws DatasetFinderException;
}
