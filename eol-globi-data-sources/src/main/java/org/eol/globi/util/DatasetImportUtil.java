package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.DatasetImporterForRSS;
import org.eol.globi.data.DatasetImporterWithListener;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.StudyImporterConfigurator;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.StudyImporterFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DatasetImportUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetImportUtil.class);

    public static void importDataset(Dataset dataset,
                                     NodeFactory nodeFactory,
                                     ImportLogger logger,
                                     File workDir) throws StudyImporterException {
        importDataset(null, dataset, nodeFactory, logger, workDir);
    }


    public static void importDataset(StudyImporterConfigurator studyImporterConfigurator,
                                     Dataset dataset,
                                     NodeFactory nodeFactory,
                                     ImportLogger logger, File workDir) throws StudyImporterException {
        importDataset(studyImporterConfigurator, dataset, nodeFactory, logger, null, workDir);
    }

    public static void importDataset(StudyImporterConfigurator studyImporterConfigurator,
                                     Dataset dataset,
                                     NodeFactory nodeFactory,
                                     ImportLogger logger,
                                     GeoNamesService geoNamesService,
                                     File workDir) throws StudyImporterException {
        nodeFactory.getOrCreateDataset(dataset);

        NodeFactory nodeFactoryForDataset = new NodeFactoryWithDatasetContext(nodeFactory, dataset);

        DatasetImporter datasetImporter = new StudyImporterFactoryImpl(nodeFactoryForDataset).createImporter(dataset);
        datasetImporter.setDataset(dataset);
        datasetImporter.setWorkDir(workDir);

        if (studyImporterConfigurator != null) {
            studyImporterConfigurator.configure(datasetImporter);
        }

        if (logger != null) {
            datasetImporter.setLogger(logger);
        }

        if (geoNamesService != null) {
            datasetImporter.setGeoNamesService(geoNamesService);
        }

        datasetImporter.importStudy();
    }

    public static void resolveAndImportDatasets(List<Dataset> datasetDependencies,
                                                List<Dataset> datasetsWithDependencies,
                                                ImportLogger logger,
                                                NodeFactory nodeFactory,
                                                String archiveLocation, File workDir) throws StudyImporterException {

        final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = DBMaker.newTempTreeMap();

        indexUnresolvedDependencies(datasetsWithDependencies, logger, nodeFactory, archiveLocation, interactionsWithUnresolvedOccurrenceIds, workDir);

        resolveDependencies(datasetDependencies, logger, nodeFactory, archiveLocation, interactionsWithUnresolvedOccurrenceIds, workDir);

        indexDatasetWithResolvedDependencies(datasetsWithDependencies, logger, nodeFactory, archiveLocation, interactionsWithUnresolvedOccurrenceIds, workDir);
    }

    private static void indexDatasetWithResolvedDependencies(List<Dataset> datasetsWithDependencies, ImportLogger logger, NodeFactory nodeFactory, String archiveLocation, Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, File workDir) throws StudyImporterException {
        final String msgPrefix = "importing datasets for [" + archiveLocation + "]";
        LOG.info(msgPrefix + "...");
        importDatasets(interactionsWithUnresolvedOccurrenceIds, datasetsWithDependencies, logger, nodeFactory, workDir);
        LOG.info(msgPrefix + " done.");
    }

    private static void resolveDependencies(List<Dataset> datasetDependencies, ImportLogger logger, NodeFactory nodeFactory, String archiveLocation, Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, File workDir) {
        final String msgPrefix1 = "indexing dependencies of [" + archiveLocation + "]";
        LOG.info(msgPrefix1 + "...");
        indexDatasets(
                datasetDependencies,
                logger,
                nodeFactory,
                new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds), workDir
        );
        pruneKeysWithEmptyValues(interactionsWithUnresolvedOccurrenceIds, logger);
        LOG.info(msgPrefix1 + " done: resolved [" + interactionsWithUnresolvedOccurrenceIds.size() + "] occurrence references");
    }

    private static void indexUnresolvedDependencies(List<Dataset> datasetsWithDependencies, ImportLogger logger, NodeFactory nodeFactory, String archiveLocation, Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, File workDir) {
        final String msgPrefix0 = "indexing unresolved occurrence references of [" + archiveLocation + "]";
        LOG.info(msgPrefix0 + "...");
        indexDatasets(
                datasetsWithDependencies,
                logger,
                nodeFactory,
                new InteractionListenerCollectUnresolvedIds(interactionsWithUnresolvedOccurrenceIds),
                workDir
        );
        LOG.info(msgPrefix0 + " done: indexed [" + interactionsWithUnresolvedOccurrenceIds.size() + "] unresolved occurrences");
    }

    private static void pruneKeysWithEmptyValues(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, ImportLogger logger) {
        for (Map.Entry<Pair<String, String>, Map<String, String>> queryResultPair : interactionsWithUnresolvedOccurrenceIds.entrySet()) {
            if (queryResultPair.getValue().isEmpty()) {
                if (logger != null) {
                    logger.warn(null, "found unresolved reference [" + queryResultPair.getKey().getValue() + "]");
                }
                interactionsWithUnresolvedOccurrenceIds.remove(queryResultPair.getKey());
            }
        }
    }

    public static void importDatasets(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets, ImportLogger logger, NodeFactory nodeFactory, File workDir) throws StudyImporterException {
        for (Dataset dataset : datasets) {
            try {
                importDataset(studyImporter -> {
                    if (studyImporter instanceof DatasetImporterWithListener) {
                        final InteractionListenerResolving interactionListener = new InteractionListenerResolving(
                                interactionsWithUnresolvedOccurrenceIds,
                                ((DatasetImporterWithListener) studyImporter).getInteractionListener());
                        ((DatasetImporterWithListener) studyImporter).setInteractionListener(interactionListener);
                    }

                }, dataset, nodeFactory, logger, workDir);
            } catch (StudyImporterException | IllegalStateException ex) {
                LogUtil.logError(logger, ex);
            }
        }
    }

    private static void indexDatasets(List<Dataset> datasets, ImportLogger logger, NodeFactory nodeFactory, InteractionListener indexingListener, File workDir) {
        for (Dataset dataset : datasets) {
            if (needsIndexing(dataset)) {
                try {
                    importDataset(studyImporter -> {
                        studyImporter.setLogger(logger);
                        if (studyImporter instanceof DatasetImporterWithListener) {
                            ((DatasetImporterWithListener) studyImporter)
                                    .setInteractionListener(indexingListener);
                        }
                    }, dataset, nodeFactory, logger, workDir);
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
