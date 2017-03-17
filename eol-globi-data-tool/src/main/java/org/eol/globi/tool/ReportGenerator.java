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
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.StudyNodeListener;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Set;

public class ReportGenerator {
    private static final Log LOG = LogFactory.getLog(ReportGenerator.class);

    public static final String GLOBI_COLLECTION_NAME = "Global Biotic Interactions";

    private final GraphDatabaseService graphService;

    private GraphDatabaseService getGraphDb() {
        return this.graphService;
    }

    public ReportGenerator(GraphDatabaseService graphService) {
        this.graphService = graphService;
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

        LOG.info("report for studies generating ...");
        generateReportForStudies();
        LOG.info("report for studies done.");
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
            public boolean matches(Study study, String sourceId2) {
                return StringUtils.equals(parse(study), sourceId2);
            }

            @Override
            public String getGroupByKeyName() {
                return StudyConstant.SOURCE_ID;
            }
        });
    }

    public void generateReportForStudies() {
        NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
            @Override
            public void onStudy(StudyNode study) {
                generateReportForStudy(study);
            }
        });

    }

    protected void generateReportForStudy(StudyNode study) {
        Set<Long> ids = new HashSet<Long>();
        HashSet<Long> idsNoMatch1 = new HashSet<>();
        generateReportForStudy(study, ids, new Counter(), idsNoMatch1);
    }

    protected void generateReportForStudy(StudyNode study, Set<Long> ids, Counter interactionCounter, HashSet<Long> idsNoMatch) {
        Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
        countInteractionsAndTaxa(specimens, ids, interactionCounter, idsNoMatch);

        Transaction tx = getGraphDb().beginTx();
        try {
            Node node = getGraphDb().createNode();
            if (StringUtils.isNotBlank(study.getSource())) {
                node.setProperty(StudyConstant.SOURCE, study.getSource());
            }
            if (StringUtils.isNotBlank(study.getSourceId())) {
                node.setProperty(StudyConstant.SOURCE_ID, study.getSourceId());
            }
            if (StringUtils.isNotBlank(study.getCitation())) {
                node.setProperty(StudyConstant.CITATION, study.getCitation());
            }
            if (StringUtils.isNotBlank(study.getDOI())) {
                node.setProperty(StudyConstant.DOI, study.getDOI());
            }
            if (StringUtils.isNotBlank(study.getExternalId())) {
                node.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, study.getExternalId());
            }
            node.setProperty(StudyConstant.TITLE, study.getTitle());
            node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, interactionCounter.getCount() / 2);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, ids.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH, idsNoMatch.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, 1);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, 1);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS, 1);
            getGraphDb().index().forNodes("reports").add(node, StudyConstant.TITLE, study.getTitle());
            getGraphDb().index().forNodes("reports").add(node, StudyConstant.SOURCE, study.getTitle());
            tx.success();
        } finally {
            tx.finish();
        }
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


    void generateReportForStudySources(SourceHandler sourceHandler) {
        final Set<String> groupByKeys = new HashSet<String>();
        NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
            @Override
            public void onStudy(StudyNode study) {
                String groupByKey = sourceHandler.parse(study);
                if (StringUtils.isNotBlank(groupByKey)) {
                    groupByKeys.add(groupByKey);
                }
            }
        });

        for (final String groupByKey : groupByKeys) {
            final Set<Long> distinctTaxonIds = new HashSet<>();
            final Set<Long> distinctTaxonIdsNoMatch = new HashSet<>();
            final Counter counter = new Counter();
            final Counter studyCounter = new Counter();
            final Set<String> distinctSources = new HashSet<String>();
            final Set<String> distinctDatasets = new HashSet<String>();

            NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
                @Override
                public void onStudy(StudyNode study) {
                    if (sourceHandler.matches(study, groupByKey)) {
                        Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
                        countInteractionsAndTaxa(specimens, distinctTaxonIds, counter, distinctTaxonIdsNoMatch);
                        studyCounter.count();
                        distinctSources.add(study.getSource());
                        distinctDatasets.add(study.getSourceId());
                    }

                }
            });

            Transaction tx = getGraphDb().beginTx();
            try {
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
            } finally {
                tx.finish();
            }
        }


    }

    void generateReportForCollection() {
        final Set<Long> distinctTaxonIds = new HashSet<Long>();
        final Set<Long> distinctTaxonIdsNoMatch = new HashSet<Long>();
        final Counter counter = new Counter();
        final Counter studyCounter = new Counter();
        final Set<String> distinctSources = new HashSet<String>();
        final Set<String> distinctDatasets = new HashSet<String>();

        NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
            @Override
            public void onStudy(StudyNode study) {
                Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
                countInteractionsAndTaxa(specimens, distinctTaxonIds, counter, distinctTaxonIdsNoMatch);
                studyCounter.count();
                distinctSources.add(study.getSource());
                distinctDatasets.add(study.getSourceId());
            }
        });

        Transaction tx = getGraphDb().beginTx();
        try {
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
        } finally {
            tx.finish();
        }


    }


    private void countInteractionsAndTaxa(Iterable<Relationship> specimens, Set<Long> ids, Counter interactionCounter, Set<Long> idsNoMatch) {
        for (Relationship specimen : specimens) {
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
        }
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
