package org.globalbioticinteractions.dataset;

import org.eol.globi.service.GitHubUtil;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class DatasetRegistryGitHubArchive extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubArchive(InputStreamFactory factory) {
        super(new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, factory);
            }
        });
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
