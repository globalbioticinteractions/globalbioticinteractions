package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Study;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.DatasetFinderGitHubRemote;
import org.eol.globi.service.GitHubImporterFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

public class StudyImporterForGitHubData extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGitHubData.class);

    public StudyImporterForGitHubData(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Collection<String> repositories;
        try {
            repositories = new DatasetFinderGitHubRemote().findNamespaces();
        } catch (DatasetFinderException e) {
            throw new StudyImporterException("failed to discover datasets", e);
        }

        for (String repository : repositories) {
            try {
                LOG.info("importing github repo [" + repository + "]...");
                importData(repository);
                LOG.info("importing github repo [" + repository + "] done.");
            } catch (StudyImporterException ex) {
                LOG.error("failed to import data from repo [" + repository + "]", ex);
            }
        }
        return null;
    }

    public void importData(String repo) throws StudyImporterException {
        try {
            StudyImporter importer = new GitHubImporterFactory().createImporter(repo, parserFactory, nodeFactory);
            if (importer != null) {
                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            }
        } catch (IOException | NodeFactoryException | URISyntaxException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        }
    }

}
