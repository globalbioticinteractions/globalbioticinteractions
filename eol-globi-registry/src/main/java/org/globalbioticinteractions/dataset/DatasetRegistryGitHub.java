package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class DatasetRegistryGitHub implements DatasetRegistry {

    private final ResourceService resourceService;

    DatasetRegistryGitHub(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public Iterable<String> findNamespaces() throws DatasetRegistryException {
        try {
            return GitHubUtil.find(this.resourceService);
        } catch (URISyntaxException | IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

    public ResourceService getResourceService() {
        return resourceService;
    }

}
