package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.InputStreamFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

public abstract class DatasetRegistryGitHub implements DatasetRegistry {

    private final InputStreamFactory inputStreamFactory;

    DatasetRegistryGitHub(InputStreamFactory inputStreamFactory) {
        this.inputStreamFactory = inputStreamFactory;
    }

    @Override
    public Collection<String> findNamespaces() throws DatasetRegistryException {
        try {
            return GitHubUtil.find(getInputStreamFactory());
        } catch (URISyntaxException | IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

    protected InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }
}
