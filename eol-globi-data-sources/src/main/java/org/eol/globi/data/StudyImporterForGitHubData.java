package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFactory;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.GitHubImporterFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StudyImporterForGitHubData extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGitHubData.class);

    private final DatasetFinder finder;

    public StudyImporterForGitHubData(ParserFactory parserFactory, NodeFactory nodeFactory, DatasetFinder finder) {
        super(parserFactory, nodeFactory);
        this.finder = finder;
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Collection<String> repositories;
        try {
            repositories = getDatasetFinder().findNamespaces();
        } catch (DatasetFinderException e) {
            throw new StudyImporterException("failed to discover datasets", e);
        }


        List<String> repositoriesWithIssues = new ArrayList<>();
        for (String repository : repositories) {
            try {
                importData(repository);
            } catch (StudyImporterException e) {
                repositoriesWithIssues.add(repository);
            }
        }

        if (repositoriesWithIssues.size() > 0) {
            throw new StudyImporterException("failed to import one or more repositories: [" + StringUtils.join(repositoriesWithIssues, ", ") + "]");
        }
    }

    public void importData(String repository) throws StudyImporterException {
        try {
            LOG.info("importing github repo [" + repository + "]...");
            Dataset dataset = DatasetFactory.datasetFor(repository, getDatasetFinder());
            nodeFactory.getOrCreateDataset(dataset);
            importData(dataset);
            LOG.info("importing github repo [" + repository + "] done.");
        } catch (StudyImporterException | DatasetFinderException ex) {
            String msg = "failed to import data from repo [" + repository + "]";
            LOG.error(msg, ex);
            throw new StudyImporterException(msg, ex);
        }
    }

    private DatasetFinder getDatasetFinder() {
        return finder;
    }

    public void importData(Dataset dataset) throws StudyImporterException {
        StudyImporter importer = new GitHubImporterFactory().createImporter(dataset, nodeFactory);
        if (importer != null) {
            if (getLogger() != null) {
                importer.setLogger(getLogger());
            }
            importer.importStudy();
        }
    }

}
