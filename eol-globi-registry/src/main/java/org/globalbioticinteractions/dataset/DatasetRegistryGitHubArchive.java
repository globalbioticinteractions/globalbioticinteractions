package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderException;
import org.globalbioticinteractions.dataset.DatasetRegistryGitHub;

import java.io.IOException;
import java.net.URISyntaxException;

public class DatasetRegistryGitHubArchive extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubArchive(InputStreamFactory factory) {
        super(factory);
    }

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            String commitSha = GitHubUtil.lastCommitSHA(namespace, getInputStreamFactory());
            return GitHubUtil.getArchiveDataset(namespace, commitSha, getInputStreamFactory());
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }
    }

}
