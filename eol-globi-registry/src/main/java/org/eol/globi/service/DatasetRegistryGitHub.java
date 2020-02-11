package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.DatasetFinderException;
import org.globalbioticinteractions.dataset.DatasetRegistry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

public abstract class DatasetRegistryGitHub implements DatasetRegistry {

    private final InputStreamFactory inputStreamFactory;

    DatasetRegistryGitHub(InputStreamFactory inputStreamFactory) {
        this.inputStreamFactory = inputStreamFactory;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetFinderException {
        try {
            return GitHubUtil.find(getInputStreamFactory());
        } catch (URISyntaxException | IOException e) {
            throw new DatasetFinderException(e);
        }
    }

    protected InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }
}
