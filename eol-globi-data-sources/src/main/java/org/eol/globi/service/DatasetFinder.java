package org.eol.globi.service;

import java.util.Collection;

public interface DatasetFinder {
    Collection<String> findNamespaces() throws DatasetFinderException;

    Dataset datasetFor(String namespace) throws DatasetFinderException;
}
