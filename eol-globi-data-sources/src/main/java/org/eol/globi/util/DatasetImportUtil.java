package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.StudyImporterConfigurator;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.DatasetImporterForRSS;
import org.eol.globi.data.DatasetImporterWithListener;
import org.eol.globi.service.StudyImporterFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.mapdb.DBMaker;

import java.util.List;
import java.util.Map;

public class DatasetImportUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetImportUtil.class);

    public static void importDataset(StudyImporterConfigurator studyImporterConfigurator, Dataset dataset, NodeFactory nodeFactory, ImportLogger logger) throws StudyImporterException {
        nodeFactory.getOrCreateDataset(dataset);
        NodeFactory nodeFactoryForDataset = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
        DatasetImporter datasetImporter = new StudyImporterFactoryImpl(nodeFactoryForDataset).createImporter(dataset);
        datasetImporter.setDataset(dataset);
        if (logger != null) {
            datasetImporter.setLogger(logger);
        }
        studyImporterConfigurator.configure(datasetImporter);
        datasetImporter.importStudy();
    }

    public static void resolveAndImportDatasets(List<Dataset> datasetDependencies,
                                                List<Dataset> datasetsWithDependencies,
                                                ImportLogger logger,
                                                NodeFactory nodeFactory,
                                                String archiveLocation) throws StudyImporterException {

        final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = DBMaker.newTempTreeMap();

        final String msgPrefix1 = "indexing dependencies of [" + archiveLocation + "]";
        LOG.info(msgPrefix1 + "...");
        indexArchives(interactionsWithUnresolvedOccurrenceIds, datasetDependencies, logger, nodeFactory);
        LOG.info(msgPrefix1 + " done: indexed [" + interactionsWithUnresolvedOccurrenceIds.size() + "] occurrences");

        final String msgPrefix = "importing datasets for [" + archiveLocation + "]";
        LOG.info(msgPrefix + "...");
        importArchives(interactionsWithUnresolvedOccurrenceIds, datasetsWithDependencies, logger, nodeFactory);
        LOG.info(msgPrefix + " done.");
    }

    public static void importArchives(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets, ImportLogger logger, NodeFactory nodeFactory) throws StudyImporterException {
        for (Dataset dataset : datasets) {
            try {
                importDataset(studyImporter -> {
                    if (studyImporter instanceof DatasetImporterWithListener) {
                        final InteractionListenerResolving interactionListener = new InteractionListenerResolving(
                                interactionsWithUnresolvedOccurrenceIds,
                                ((DatasetImporterWithListener) studyImporter).getInteractionListener());
                        ((DatasetImporterWithListener) studyImporter).setInteractionListener(interactionListener);
                    }

                }, dataset, nodeFactory, logger);
            } catch (StudyImporterException | IllegalStateException ex) {
                LogUtil.logError(logger, ex);
            }
        }
    }

    private static void indexArchives(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets, ImportLogger logger, NodeFactory nodeFactory) throws StudyImporterException {

        final InteractionListenerIndexing indexingListener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);
        for (Dataset dataset : datasets) {
            if (needsIndexing(dataset)) {
                try {
                    importDataset(studyImporter -> {
                        studyImporter.setLogger(logger);
                        if (studyImporter instanceof DatasetImporterWithListener) {
                            ((DatasetImporterWithListener) studyImporter)
                                    .setInteractionListener(indexingListener);
                        }
                    }, dataset, nodeFactory, logger);
                } catch (StudyImporterException | IllegalStateException ex) {
                    LogUtil.logError(logger, ex);
                }
            }
        }
    }

    public static boolean needsIndexing(Dataset dataset) {
        return StringUtils.equals(dataset.getOrDefault(DatasetImporterForRSS.HAS_DEPENDENCIES, null), "true");
    }

}
