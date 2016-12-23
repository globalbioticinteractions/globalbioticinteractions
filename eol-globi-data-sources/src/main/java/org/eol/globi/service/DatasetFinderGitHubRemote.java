package org.eol.globi.service;

import org.eol.globi.data.StudyImporterException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DatasetFinderGitHubRemote extends DatasetFinderGitHub {

    @Override
    public Dataset datasetFor(String namespace) throws DatasetFinderException {
        try {
            return new DatasetImpl(namespace, URI.create(GitHubUtil.getBaseUrlLastCommit(namespace)));
        } catch (URISyntaxException | IOException | StudyImporterException e) {
            throw new DatasetFinderException(e);
        }
    }

}
