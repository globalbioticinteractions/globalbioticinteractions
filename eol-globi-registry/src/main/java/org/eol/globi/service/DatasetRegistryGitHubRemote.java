package org.eol.globi.service;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderException;
import org.globalbioticinteractions.dataset.DatasetImpl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DatasetRegistryGitHubRemote extends DatasetRegistryGitHub {

    public DatasetRegistryGitHubRemote(InputStreamFactory factory) {
        super(factory);
    }


    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            String baseUrlLastCommit = GitHubUtil.getBaseUrlLastCommit(namespace, getInputStreamFactory());
            return new DatasetImpl(namespace, URI.create(baseUrlLastCommit), getInputStreamFactory());
        } catch (URISyntaxException | IOException | StudyImporterException e) {
            throw new DatasetFinderException(e);
        }
    }

}
