package org.eol.globi.service;

import java.util.Collection;
import java.util.Collections;

public class DatasetRegistryGitHubArchiveMaster implements DatasetRegistry {
    private final Collection<String> namespaces;

    public DatasetRegistryGitHubArchiveMaster() {
        this(Collections.emptyList());
    }

    public DatasetRegistryGitHubArchiveMaster(Collection<String> namespaces) {
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
