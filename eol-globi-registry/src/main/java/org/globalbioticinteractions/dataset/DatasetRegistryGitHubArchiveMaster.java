package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;

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
    public Collection<String> findNamespaces() throws DatasetRegistryException {
        return namespaces;
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        return GitHubUtil.getArchiveDataset(namespace, "master", inStream -> inStream);
    }

}
