package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jena.atlas.iterator.Iter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFactory;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.eol.globi.service.StudyImporterFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class DatasetImporterForRegistry extends NodeBasedImporter {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetImporterForRegistry.class);

    private final DatasetRegistry registry;
    private Predicate<Dataset> datasetFilter = x -> true;

    public DatasetImporterForRegistry(ParserFactory parserFactory, NodeFactory nodeFactory, DatasetRegistry registry) {
        super(parserFactory, nodeFactory);
        this.registry = registry;
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Iterable<String> namespaces;
        try {
            namespaces = getRegistry().findNamespaces();
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

    private void importData(String namespace) throws StudyImporterException {
        try {
            LOG.info("[" + namespace + "] checking status...");
            Dataset dataset = new DatasetFactory(getRegistry()).datasetFor(namespace);
            if (datasetFilter.test(dataset)) {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                LOG.info("[" + namespace + "] is active, importing...");
                getNodeFactory().getOrCreateDataset(dataset);
                importData(dataset);
                stopWatch.stop();
                LOG.info("[" + namespace + "] imported in " + stopWatch.getTime(TimeUnit.SECONDS) + "s");
            } else {
                LOG.info("[" + namespace + "] is deprecated, not importing.");
            }
        } catch (StudyImporterException | DatasetRegistryException ex) {
            String msg = "failed to import data from repo [" + namespace + "]";
            LOG.error(msg, ex);
            throw new StudyImporterException(msg, ex);
        }
    }

    private DatasetRegistry getRegistry() {
        return registry;
    }

    public void importData(Dataset dataset) throws StudyImporterException {
        DatasetImporter importer = new StudyImporterFactoryImpl(getNodeFactory()).createImporter(dataset);
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

    public void setDatasetFilter(Predicate<Dataset> datasetFilter) {
        this.datasetFilter = datasetFilter;
    }
}
