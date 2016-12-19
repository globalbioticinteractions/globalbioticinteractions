package org.eol.globi.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

public class DatasetFinderGitHub implements DatasetFinder {
    @Override
    public Collection<String> find() throws DatasetFinderException {
        try {
            return GitHubUtil.find();
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }

    }

    @Override
    public URL archiveUrlFor(String repo) throws DatasetFinderException {
        try {
            String commitSha = GitHubUtil.lastCommitSHA(repo);
            return new URL("https://github.com/" + repo + "/archive/" + commitSha + ".zip");
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }
    }

}
