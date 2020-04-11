package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.StudyNodeListener;
import org.mapdb.DB;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ReportGenerator {
    private static final Log LOG = LogFactory.getLog(ReportGenerator.class);

    public static final String GLOBI_COLLECTION_NAME = "Global Biotic Interactions";

    private final GraphDatabaseService graphService;
    private final CacheService cacheService;

    private GraphDatabaseService getGraphDb() {
        return this.graphService;
    }

    public ReportGenerator(GraphDatabaseService graphService) {
        this.graphService = graphService;
        this.cacheService = new CacheService();
    }

    public void run() {
        LOG.info("report for collection generating ...");
        generateReportForCollection();
        LOG.info("report for collection done.");

        LOG.info("report for source citations generating ...");
        generateReportForSourceCitations();
        LOG.info("report for source citations done.");

        LOG.info("report for sources generating ...");
        generateReportForSourceIndividuals();
        LOG.info("report for sources done.");

        LOG.info("report for source organizations generating ...");
        generateReportForSourceOrganizations();
        LOG.info("report for source organizations done.");
    }

    public void generateReportForSourceCitations() {
        generateReportForStudySources(new SourceHandler() {
            @Override
            public String parse(Study study) {
                return study.getSource();
            }

            @Override
            public boolean matches(Study study, String sourceId) {
                return StringUtils.equals(parse(study), sourceId);
            }

            @Override
            public String getGroupByKeyName() {
                return StudyConstant.SOURCE;
            }
        });
    }

    public void generateReportForSourceIndividuals() {
        generateReportForStudySources(new SourceHandlerBasic());
    }

    public void generateReportForSourceOrganizations() {
        generateReportForStudySources(new SourceHandler() {
            @Override
            public String parse(Study source) {
                return StringUtils.split(source.getSourceId(), "/")[0];
            }

            @Override
            public boolean matches(Study study, String sourceId) {
                return StringUtils.equals(parse(study), sourceId);
            }

            @Override
            public String getGroupByKeyName() {
                return StudyConstant.SOURCE_ID;
            }
        });
    }

    interface SourceHandler {
        String parse(Study study);

        boolean matches(Study study, String sourceId);

        String getGroupByKeyName();
    }

    private static class SourceHandlerBasic implements SourceHandler {
        @Override
        public String parse(Study study) {
            return study.getSourceId();
        }

        @Override
        public boolean matches(Study study, String sourceId) {
            return StringUtils.equals(parse(study), sourceId);
        }

        @Override
        public String getGroupByKeyName() {
            return StudyConstant.SOURCE_ID;
        }
    }


    private void generateReportForStudySources(SourceHandler sourceHandler) {
        try {
            DB reportCache = cacheService.initDb("sourceReports" + UUID.randomUUID());
            reportForHandler(sourceHandler, reportCache);
            reportCache.close();
        } catch (PropertyEnricherException e) {
            LOG.warn("failed to create report", e);
        }

    }

    private void reportForHandler(SourceHandler sourceHandler, DB reportCache) {
        final Set<String> groupByKeys = reportCache
                .createHashSet("groupByKeys")
                .make();

        NodeUtil.findStudies(getGraphDb(), study -> {
            String groupByKey = sourceHandler.parse(study);
            if (StringUtils.isNotBlank(groupByKey)) {
                groupByKeys.add(groupByKey);
            }
        });
        final Set<Long> distinctTaxonIds = reportCache
                .createHashSet("distinctTaxonIds")
                .make();

        final Set<Long> distinctTaxonIdsNoMatch = reportCache
                .createHashSet("distinctTaxonIdsNoMatch")
                .make();

        final Set<String> distinctSources = reportCache
                .createHashSet("distinctSource")
                .make();

        final Set<String> distinctDatasets = reportCache
                .createHashSet("distinctDatasets")
                .make();

        for (final String groupByKey : groupByKeys) {
            final Counter counter = new Counter();
            final Counter studyCounter = new Counter();
            distinctDatasets.clear();
            distinctSources.clear();
            distinctTaxonIds.clear();
            distinctTaxonIdsNoMatch.clear();

            NodeUtil.findStudies(getGraphDb(), study -> {
                if (sourceHandler.matches(study, groupByKey)) {
                    countInteractionsAndTaxa(study, distinctTaxonIds, counter, distinctTaxonIdsNoMatch);
                    studyCounter.count();
                    distinctSources.add(study.getSource());
                    distinctDatasets.add(study.getSourceId());
                }

            });

            try (Transaction tx = getGraphDb().beginTx()) {
                final Node node = getGraphDb().createNode();
                node.setProperty(sourceHandler.getGroupByKeyName(), groupByKey);
                node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH, distinctTaxonIdsNoMatch.size());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, distinctSources.size());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS, distinctDatasets.size());

                getGraphDb().index().forNodes("reports").add(node, sourceHandler.getGroupByKeyName(), groupByKey);
                tx.success();
            }
        }
    }

    void generateReportForCollection() {
        try {
            DB reportCache = cacheService.initDb("collectionReport");
            generateCollectionReport(reportCache);
            reportCache.close();
        } catch (PropertyEnricherException e) {
            LOG.warn("failed to generate collection report", e);
        }
    }

    private void generateCollectionReport(DB reportCache) {
        final Set<Long> distinctTaxonIds = reportCache.createHashSet("distinctTaxonIds").make();
        final Set<Long> distinctTaxonIdsNoMatch = reportCache.createHashSet("distinctTaxonIdsNoMatch").make();
        final Counter counter = new Counter();
        final Counter studyCounter = new Counter();
        final Set<String> distinctSources = reportCache.createHashSet("distinctSources").make();
        final Set<String> distinctDatasets = reportCache.createHashSet("distinctDatasets").make();

        NodeUtil.findStudies(getGraphDb(), study -> {
            countInteractionsAndTaxa(study, distinctTaxonIds, counter, distinctTaxonIdsNoMatch);
            studyCounter.count();
            distinctSources.add(study.getSource());
            distinctDatasets.add(study.getSourceId());
        });

        try (Transaction tx = getGraphDb().beginTx()) {
            final Node node = getGraphDb().createNode();
            node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH, distinctTaxonIdsNoMatch.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, distinctSources.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS, distinctDatasets.size());
            getGraphDb().index().forNodes("reports").add(node, PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            tx.success();
        }
    }


    private void countInteractionsAndTaxa(StudyNode study, Set<Long> ids, Counter interactionCounter, Set<Long> idsNoMatch) {

        NodeUtil.RelationshipListener handler = specimen -> {
            Iterable<Relationship> relationships = specimen.getEndNode().getRelationships();
            for (Relationship relationship : relationships) {
                InteractType[] types = InteractType.values();
                for (InteractType type : types) {
                    if (relationship.isType(NodeUtil.asNeo4j(type)) && !relationship.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                        interactionCounter.count();
                        break;
                    }
                }
            }
            Relationship classifiedAs = specimen.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
            if (classifiedAs != null) {
                Node taxonNode = classifiedAs.getEndNode();
                ids.add(taxonNode.getId());
                if (!TaxonUtil.isResolved(new TaxonNode(taxonNode))) {
                    idsNoMatch.add(taxonNode.getId());
                }
            }

        };

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler);
    }

    private static class Counter {
        int counter = 0;

        public void count() {
            counter++;
        }

        public int getCount() {
            return counter;
        }
    }

}
