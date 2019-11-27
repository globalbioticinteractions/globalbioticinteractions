package org.eol.globi.service;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.InputStreamFactory;

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
            return new DatasetImpl(namespace, URI.create(GitHubUtil.getBaseUrlLastCommit(namespace)), getInputStreamFactory());
        } catch (URISyntaxException | IOException | StudyImporterException e) {
            throw new DatasetFinderException(e);
        }
    }

}
