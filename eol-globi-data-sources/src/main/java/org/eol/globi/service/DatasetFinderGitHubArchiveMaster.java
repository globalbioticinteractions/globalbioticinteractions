package org.eol.globi.service;

import java.util.Collection;
import java.util.Collections;

public class DatasetFinderGitHubArchiveMaster implements DatasetFinder {
    private final Collection<String> namespaces;

    public DatasetFinderGitHubArchiveMaster() {
        this(Collections.emptyList());
    }

    public DatasetFinderGitHubArchiveMaster(Collection<String> namespaces) {
        this.namespaces = Collections.unmodifiableCollection(namespaces);
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        return namespaces;
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        return GitHubUtil.getArchiveDataset(namespace, "master");
    }

}
