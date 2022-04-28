package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceServiceHTTP;

import java.io.IOException;

public class DatasetRegistryGitHubArchive extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubArchive(ResourceService resourceService) {
        super(resourceService);
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        try {
            String commitSha = GitHubUtil.lastCommitSHA(namespace, getResourceService());
            return GitHubUtil.getArchiveDataset(namespace, commitSha, getResourceService());
        } catch (IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

}
