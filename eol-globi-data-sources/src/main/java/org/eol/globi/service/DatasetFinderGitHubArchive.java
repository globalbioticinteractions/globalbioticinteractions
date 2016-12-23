package org.eol.globi.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DatasetFinderGitHubArchive extends DatasetFinderGitHub {

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            String commitSha = GitHubUtil.lastCommitSHA(namespace);
            return new DatasetImpl(namespace, URI.create("https://github.com/" + namespace + "/archive/" + commitSha + ".zip"));
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }
    }

}
