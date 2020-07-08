package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.InteractionListener;
import org.eol.globi.data.LogUtil;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterConfigurator;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForRSS;
import org.eol.globi.data.StudyImporterForTSV;
import org.eol.globi.data.StudyImporterWithListener;
import org.eol.globi.service.StudyImporterFactoryImpl;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.mapdb.DBMaker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DatasetImportUtil {
    private static final Log LOG = LogFactory.getLog(DatasetImportUtil.class);

    public static void importDataset(StudyImporterConfigurator studyImporterConfigurator, Dataset dataset, NodeFactory nodeFactory, ImportLogger logger) throws StudyImporterException {
        nodeFactory.getOrCreateDataset(dataset);
        NodeFactory nodeFactoryForDataset = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
        StudyImporter studyImporter = new StudyImporterFactoryImpl(nodeFactoryForDataset).createImporter(dataset);
        studyImporter.setDataset(dataset);
        if (logger != null) {
            studyImporter.setLogger(logger);
        }
        studyImporterConfigurator.configure(studyImporter);
        studyImporter.importStudy();
    }

    public static void resolveAndImportDatasets(List<Dataset> datasetDependencies,
                                                List<Dataset> datasetsWithDependencies,
                                                ImportLogger logger,
                                                NodeFactory nodeFactory,
                                                String archiveLocation) throws StudyImporterException {

        final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = DBMaker.newTempTreeMap();

        final String msgPrefix1 = "indexing dependencies of [" + archiveLocation + "]";
        LOG.info(msgPrefix1 + "...");
        indexArchives(interactionsWithUnresolvedOccurrenceIds, datasetDependencies, logger, nodeFactory);
        LOG.info(msgPrefix1 + " done: indexed [" + interactionsWithUnresolvedOccurrenceIds.size() + "] occurrences");

        final String msgPrefix = "importing datasets for [" + archiveLocation + "]";
        LOG.info(msgPrefix + "...");
        importArchives(interactionsWithUnresolvedOccurrenceIds, datasetsWithDependencies, logger, nodeFactory);
        LOG.info(msgPrefix + " done.");
    }

    public static void importArchives(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets, ImportLogger logger, NodeFactory nodeFactory) throws StudyImporterException {
        for (Dataset dataset : datasets) {
            try {
                importDataset(studyImporter -> {
                    if (studyImporter instanceof StudyImporterWithListener) {
                        final EnrichingInteractionListener interactionListener = new EnrichingInteractionListener(
                                interactionsWithUnresolvedOccurrenceIds,
                                ((StudyImporterWithListener) studyImporter).getInteractionListener());
                        ((StudyImporterWithListener) studyImporter).setInteractionListener(interactionListener);
                    }

                }, dataset, nodeFactory, logger);
            } catch (StudyImporterException | IllegalStateException ex) {
                LogUtil.logError(logger, ex);
            }
        }
    }

    public static void indexArchives(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, List<Dataset> datasets, ImportLogger logger, NodeFactory nodeFactory) throws StudyImporterException {

        final IndexingInteractionListener indexingListener = new IndexingInteractionListener(interactionsWithUnresolvedOccurrenceIds);
        for (Dataset dataset : datasets) {
            if (needsIndexing(dataset)) {
                try {
                    importDataset(studyImporter -> {
                        studyImporter.setLogger(logger);
                        if (studyImporter instanceof StudyImporterWithListener) {
                            ((StudyImporterWithListener) studyImporter)
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
        return StringUtils.equals(dataset.getOrDefault(StudyImporterForRSS.HAS_DEPENDENCIES, null), "true");
    }

    public static class EnrichingInteractionListener implements InteractionListener {
        private final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;
        private final InteractionListener interactionListener;

        public EnrichingInteractionListener(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, InteractionListener interactionListener) {
            this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
            this.interactionListener = interactionListener;
        }

        @Override
        public void newLink(Map<String, String> link) throws StudyImporterException {
            Map<String, String> enrichedProperties = null;
            if (link.containsKey(StudyImporterForTSV.TARGET_OCCURRENCE_ID)) {
                String targetOccurrenceId = link.get(StudyImporterForTSV.TARGET_OCCURRENCE_ID);
                Map<String, String> targetProperties = interactionsWithUnresolvedOccurrenceIds.get(targetOccurrenceId);
                if (targetProperties != null) {
                    TreeMap<String, String> enrichedMap = new TreeMap<>(link);
                    enrichProperties(targetProperties, enrichedMap, TaxonUtil.SOURCE_TAXON_NAME, TaxonUtil.TARGET_TAXON_NAME);
                    enrichProperties(targetProperties, enrichedMap, TaxonUtil.SOURCE_TAXON_ID, TaxonUtil.TARGET_TAXON_ID);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME, StudyImporterForTSV.TARGET_LIFE_STAGE_NAME);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_LIFE_STAGE_ID, StudyImporterForTSV.TARGET_LIFE_STAGE_ID);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_BODY_PART_NAME, StudyImporterForTSV.TARGET_BODY_PART_NAME);
                    enrichProperties(targetProperties, enrichedMap, StudyImporterForTSV.SOURCE_BODY_PART_ID, StudyImporterForTSV.TARGET_BODY_PART_ID);
                    enrichedProperties = enrichedMap;
                }
            }
            interactionListener.newLink(enrichedProperties == null ? link : enrichedProperties);
        }

        public void enrichProperties(Map<String, String> targetProperties, TreeMap<String, String> enrichedMap, String sourceKey, String targetKey) {
            String value = targetProperties.get(sourceKey);
            if (StringUtils.isNotBlank(value)) {
                enrichedMap.put(targetKey, value);
            }
        }
    }

    public static class IndexingInteractionListener implements InteractionListener {
        private final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;

        public IndexingInteractionListener(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
            this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
        }

        @Override
        public void newLink(Map<String, String> link) throws StudyImporterException {

            if (link.containsKey(StudyImporterForTSV.TARGET_OCCURRENCE_ID)
                    && link.containsKey(StudyImporterForTSV.SOURCE_OCCURRENCE_ID)) {
                String value = link.get(StudyImporterForTSV.SOURCE_OCCURRENCE_ID);

                if (StringUtils.startsWith(value, "http://arctos.database.museum/guid/")) {
                    String[] splitValue = StringUtils.split(value, "?");
                    value = splitValue.length == 1 ? value : splitValue[0];
                }
                interactionsWithUnresolvedOccurrenceIds.put(value, new HashMap<>(link));
            }
        }
    }
}