package org.eol.globi.service;

import java.net.URL;
import java.util.Collection;

interface DatasetFinder {
    Collection<String> find() throws DatasetFinderException;

    URL archiveUrlFor(String repo) throws DatasetFinderException;
}
