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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StudyImporterForGitHubData extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGitHubData.class);
    private DatasetFinder finder = null;

    public StudyImporterForGitHubData(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Collection<String> repositories;
        try {
            repositories = getDatasetFinder().findNamespaces();
        } catch (DatasetFinderException e) {
            throw new StudyImporterException("failed to discover datasets", e);
        }

        for (String repository : repositories) {
            importData(repository);
        }

        return null;
    }

    public void importData(String repository) {
        try {
            LOG.info("importing github repo [" + repository + "]...");
            Dataset dataset = DatasetFactory.datasetFor(repository, getDatasetFinder());
            importData(dataset);
            LOG.info("importing github repo [" + repository + "] done.");
        } catch (StudyImporterException | DatasetFinderException ex) {
            LOG.error("failed to import data from repo [" + repository + "]", ex);
        }
    }

    private DatasetFinder getDatasetFinder() {
        if (finder == null) {
            List<DatasetFinder> finders = Arrays.asList(new DatasetFinderZenodo(), new DatasetFinderGitHubArchive());
            finder = new DatasetFinderCaching(new DatasetFinderProxy(finders));
        }
        return finder;
    }

    public void importData(Dataset repo) throws StudyImporterException {
        StudyImporter importer = new GitHubImporterFactory().createImporter(repo, parserFactory, nodeFactory);
        if (importer != null) {
            if (getLogger() != null) {
                importer.setLogger(getLogger());
            }
            importer.importStudy();
        }
    }

}
