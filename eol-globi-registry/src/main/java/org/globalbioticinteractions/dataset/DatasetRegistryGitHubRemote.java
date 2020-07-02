package org.globalbioticinteractions.dataset;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.InputStreamFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DatasetRegistryGitHubRemote extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubRemote(InputStreamFactory factory) {
        super(factory);
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
        try {
            String baseUrlLastCommit = GitHubUtil.getBaseUrlLastCommit(namespace, getInputStreamFactory());
            return new DatasetImpl(namespace, URI.create(baseUrlLastCommit), getInputStreamFactory());
        } catch (URISyntaxException | IOException e) {
            throw new DatasetRegistryException(e);
        }
    }

}
