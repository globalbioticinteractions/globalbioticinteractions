package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFactory;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.eol.globi.service.StudyImporterFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StudyImporterForRegistry extends NodeBasedImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForRegistry.class);

    private final DatasetRegistry finder;

    public StudyImporterForRegistry(ParserFactory parserFactory, NodeFactory nodeFactory, DatasetRegistry finder) {
        super(parserFactory, nodeFactory);
        this.finder = finder;
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Collection<String> namespaces;
        try {
            namespaces = getDatasetFinder().findNamespaces();
        } catch (DatasetRegistryException e) {
            throw new StudyImporterException("failed to discover datasets", e);
        }


        List<String> repositoriesWithIssues = new ArrayList<>();
        for (String namespace : namespaces) {
            try {
                importData(namespace);
            } catch (StudyImporterException e) {
                repositoriesWithIssues.add(namespace);
            }
        }

        if (repositoriesWithIssues.size() > 0) {
            throw new StudyImporterException("failed to import one or more repositories: [" + StringUtils.join(repositoriesWithIssues, ", ") + "]");
        }
    }

    public void importData(String namespace) throws StudyImporterException {
        try {
            LOG.info("importing github repo [" + namespace + "]...");
            Dataset dataset = new DatasetFactory(getDatasetFinder()).datasetFor(namespace);
            getNodeFactory().getOrCreateDataset(dataset);
            importData(dataset);
            LOG.info("importing github repo [" + namespace + "] done.");
        } catch (StudyImporterException | DatasetRegistryException ex) {
            String msg = "failed to import data from repo [" + namespace + "]";
            LOG.error(msg, ex);
            throw new StudyImporterException(msg, ex);
        }
    }

    private DatasetRegistry getDatasetFinder() {
        return finder;
    }

    public void importData(Dataset dataset) throws StudyImporterException {
        StudyImporter importer = new StudyImporterFactoryImpl(getNodeFactory()).createImporter(dataset);
        if (importer != null) {
            if (getLogger() != null) {
                importer.setLogger(getLogger());
            }
            if (getGeoNamesService() != null) {
                importer.setGeoNamesService(getGeoNamesService());
            }
            importer.importStudy();
        }
    }

}
