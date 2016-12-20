package org.eol.globi.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

public abstract class DatasetFinderGitHub implements DatasetFinder {
    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        try {
            return GitHubUtil.find();
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }

    }


}
