package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.RelationshipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.mapdb.DB;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class CmdGenerateReport implements Cmd {
    private static final Logger LOG = LoggerFactory.getLogger(CmdGenerateReport.class);

    private static final String GLOBI_COLLECTION_NAME = "Global Biotic Interactions";

    private final GraphDatabaseService graphService;
    private final CacheService cacheService;

    private GraphDatabaseService getGraphDb() {
        return this.graphService;
    }

    public CmdGenerateReport(GraphDatabaseService graphService) {
        this(graphService, new CacheService());
    }

    public CmdGenerateReport(GraphDatabaseService graphService, CacheService cacheService) {
        this.graphService = graphService;
        this.cacheService = cacheService;
    }

    public void run() {
        run(LOG);
    }

    public void run(Logger log) {

        TransactionPerBatch transactionPerBatch = new TransactionPerBatch(graphService);
        transactionPerBatch.onStartBatch();
        log.info("report for collection generating ...");
        generateReportForCollection();
        log.info("report for collection done.");

        transactionPerBatch.onStartBatch();
        log.info("report for sources generating ...");
        generateReportForSourceIndividuals();
        log.info("report for sources done.");

        transactionPerBatch.onStartBatch();
        log.info("report for source organizations generating ...");
        generateReportForSourceOrganizations();
        log.info("report for source organizations done.");

        transactionPerBatch.onFinishBatch();
    }

    void generateReportForSourceIndividuals() {
        generateReportForStudySources(new NamespaceHandler() {
            @Override
            public String parse(String namespace) {
                return namespace;
            }

            @Override
            public String datasetQueryFor(String namespace) {
                return StringUtils.replace(namespace, "/", "\\/");
            }

            @Override
            public String getNamespaceKey() {
                return StudyConstant.SOURCE_ID;
            }
        });
    }

    void generateReportForSourceOrganizations() {
        generateReportForStudySources(new NamespaceHandler() {
            @Override
            public String parse(String namespace) {
                return StringUtils.split(namespace, "/")[0];
            }

            @Override
            public String datasetQueryFor(String namespace) {
                return namespace + "\\/*";
            }

            @Override
            public String getNamespaceKey() {
                return StudyConstant.SOURCE_ID;
            }
        });
    }

    interface NamespaceHandler {
        String parse(String namespace);

        String datasetQueryFor(String namespace);

        String getNamespaceKey();
    }


    private void generateReportForStudySources(NamespaceHandler namespaceHandler) {
        try {
            DB reportCache = cacheService.initDb("sourceReports" + UUID.randomUUID());
            reportForHandler(namespaceHandler, reportCache);
            reportCache.close();
        } catch (IOException e) {
            LOG.warn("failed to create report", e);
        }

    }

    private void reportForHandler(NamespaceHandler namespaceHandler, DB reportCache) {
        final Set<String> namespaceGroups = reportCache
                .createHashSet("namespaces")
                .make();

        // collect distinct namespaces
        NodeUtil.findDatasetsByQuery(getGraphDb(), dataset -> {
            String namespace = dataset.getNamespace();
            if (StringUtils.isNotBlank(namespace)) {
                namespaceGroups.add(namespaceHandler.parse(namespace));
            }
        }, "namespace", "*");

        final Set<Long> distinctTaxonIds = reportCache
                .createHashSet("distinctTaxonIds")
                .make();

        final Set<Long> distinctTaxonIdsNoMatch = reportCache
                .createHashSet("distinctTaxonIdsNoMatch")
                .make();

        final Set<String> distinctDatasets = reportCache
                .createHashSet("distinctDatasets")
                .make();

        final Set<String> distinctSources = reportCache
                .createHashSet("distinctSources")
                .make();

        for (final String namespaceGroup : namespaceGroups) {
            final Counter counter = new Counter();
            final Counter studyCounter = new Counter();
            distinctDatasets.clear();
            distinctSources.clear();
            distinctTaxonIds.clear();
            distinctTaxonIdsNoMatch.clear();

            NodeUtil.findDatasetsByQuery(getGraphDb(), dataset -> {
                Iterable<Relationship> studiesInDataset = dataset.getUnderlyingNode().getRelationships(
                        Direction.INCOMING,
                        NodeUtil.asNeo4j(RelTypes.IN_DATASET));
                for (Relationship studyInDataset : studiesInDataset) {
                    StudyNode study = new StudyNode(studyInDataset.getStartNode());
                    countInteractionsAndTaxa(distinctTaxonIds, counter, distinctTaxonIdsNoMatch, study.getUnderlyingNode());
                    studyCounter.count();
                    final String namespace = dataset.getNamespace();
                    distinctSources.add(namespace);
                    distinctDatasets.add(namespace);
                }
            }, "namespace", namespaceHandler.datasetQueryFor(namespaceGroup));

            final Node node = getGraphDb().createNode();
            String sourceIdPrefix = "globi:" + namespaceGroup;
            node.setProperty(namespaceHandler.getNamespaceKey(), sourceIdPrefix);
            node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH, distinctTaxonIdsNoMatch.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, distinctSources.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS, distinctDatasets.size());

            getGraphDb()
                    .index()
                    .forNodes("reports")
                    .add(node, namespaceHandler.getNamespaceKey(), sourceIdPrefix);

        }
    }

    void generateReportForCollection() {
        try {
            DB reportCache = cacheService.initDb("collectionReport");
            generateCollectionReport(reportCache);
            reportCache.close();
        } catch (IOException e) {
            LOG.warn("failed to generate collection report", e);
        }
    }

    private void generateCollectionReport(DB reportCache) {
        final Set<Long> distinctTaxonIds = makeOrRemake(reportCache, "distinctTaxonIds");
        final Set<Long> distinctTaxonIdsNoMatch = makeOrRemake(reportCache,"distinctTaxonIdsNoMatch");
        final Counter counter = new Counter();
        final Counter studyCounter = new Counter();
        final Set<String> distinctSources = makeOrRemakeString(reportCache,"distinctSources");
        final Set<String> distinctDatasets = makeOrRemakeString(reportCache,"distinctDatasets");

        NodeUtil.findStudies(getGraphDb(), studyNode -> {
            countInteractionsAndTaxa(distinctTaxonIds, counter, distinctTaxonIdsNoMatch, studyNode);
            studyCounter.count();
            final Dataset originatingDataset = new StudyNode(studyNode).getOriginatingDataset();
            if (originatingDataset != null) {
                final String namespace = originatingDataset.getNamespace();
                distinctSources.add(namespace);
                distinctDatasets.add(namespace);
            }
        });

        final Node node = getGraphDb().createNode();
        node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
        node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
        node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
        node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH, distinctTaxonIdsNoMatch.size());
        node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
        node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, distinctSources.size());
        node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS, distinctDatasets.size());
        getGraphDb().index().forNodes("reports")
                .add(node, PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
    }

    private Set<Long> makeOrRemake(DB reportCache, String setName) {
        if (reportCache.exists(setName)) {
            reportCache.delete(setName);
        }
        return reportCache.createHashSet(setName).make();
    }

    private Set<String> makeOrRemakeString(DB reportCache, String setName) {
        if (reportCache.exists(setName)) {
            reportCache.delete(setName);
        }
        return reportCache.createHashSet(setName).make();
    }


    private void countInteractionsAndTaxa(Set<Long> ids, Counter interactionCounter, Set<Long> idsNoMatch, Node studyNode) {

        RelationshipListener handler = specimen -> {
            Iterable<Relationship> relationships = specimen.getEndNode().getRelationships();
            for (Relationship relationship : relationships) {
                InteractType[] types = InteractType.values();
                for (InteractType type : types) {
                    if (relationship.isType(NodeUtil.asNeo4j(type))
                            && !relationship.hasProperty(PropertyAndValueDictionary.INVERTED)) {
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

        NodeUtil.handleCollectedRelationshipsNoTx(
                new NodeTypeDirection(studyNode),
                handler);
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
