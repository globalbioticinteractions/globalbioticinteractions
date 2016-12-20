package org.eol.globi.service;

import java.util.Collection;

interface DatasetFinder {
    Collection<String> findNamespaces() throws DatasetFinderException;

    Dataset datasetFor(String namespace) throws DatasetFinderException;
}
