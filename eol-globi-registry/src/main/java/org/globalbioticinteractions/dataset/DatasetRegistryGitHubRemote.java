package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class DatasetRegistryGitHubRemote extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubRemote(InputStreamFactory factory) {
        super(new ResourceServiceHTTP(factory));
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        try {
            String baseUrlLastCommit = GitHubUtil.getBaseUrlLastCommit(namespace, getResourceService());
            return new DatasetImpl(namespace, getResourceService(), URI.create(baseUrlLastCommit));
        } catch (URISyntaxException | IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

}
