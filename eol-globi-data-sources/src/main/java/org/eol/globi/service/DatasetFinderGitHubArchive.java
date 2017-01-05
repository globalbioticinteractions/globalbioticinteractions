package org.eol.globi.service;

import java.io.IOException;
import java.net.URISyntaxException;

public class DatasetFinderGitHubArchive extends DatasetFinderGitHub {

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            String commitSha = GitHubUtil.lastCommitSHA(namespace);
            return GitHubUtil.getArchiveDataset(namespace, commitSha);
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }
    }

}
