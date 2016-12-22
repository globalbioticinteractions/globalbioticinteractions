package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Study;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFactory;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderCaching;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.DatasetFinderGitHubArchive;
import org.eol.globi.service.DatasetFinderProxy;
import org.eol.globi.service.DatasetFinderZenodo;
import org.eol.globi.service.GitHubImporterFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StudyImporterForGitHubData extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGitHubData.class);

    public StudyImporterForGitHubData(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        DatasetFinderCaching finder = createDatasetFinder();
        Collection<String> repositories;
        try {
            repositories = finder.findNamespaces();

        } catch (DatasetFinderException e) {
            throw new StudyImporterException("failed to discover datasets", e);
        }

        for (String repository : repositories)
            try {
                LOG.info("importing github repo [" + repository + "]...");
                Dataset dataset = DatasetFactory.datasetFor(repository, finder);
                importData(dataset);
                LOG.info("importing github repo [" + repository + "] done.");
            } catch (StudyImporterException | DatasetFinderException ex) {
                LOG.error("failed to import data from repo [" + repository + "]", ex);
            }
        return null;
    }

    private DatasetFinderCaching createDatasetFinder() {
        List<DatasetFinder> finders = Arrays.asList(new DatasetFinderZenodo(), new DatasetFinderGitHubArchive());
        return new DatasetFinderCaching(new DatasetFinderProxy(finders));
    }

    public void importData(Dataset repo) throws StudyImporterException {
        try {
            StudyImporter importer = new GitHubImporterFactory().createImporter(repo, parserFactory, nodeFactory);
            if (importer != null) {
                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            }
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        }
    }

}
