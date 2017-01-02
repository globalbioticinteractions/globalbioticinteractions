package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyNode;
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

        LOG.info("report for sources generating ...");
        generateReportForStudySources();
        LOG.info("report for sources done.");

        LOG.info("report for studies generating ...");
        generateReportForStudies();
        LOG.info("report for studies done.");
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
        generateReportForStudy(study, ids, new Counter());
    }

    protected void generateReportForStudy(StudyNode study, Set<Long> ids, Counter interactionCounter) {
        Iterable<Relationship> specimens = study.getSpecimens();
        countInteractionsAndTaxa(specimens, ids, interactionCounter);

        Transaction tx = getGraphDb().beginTx();
        try {
            Node node = getGraphDb().createNode();
            node.setProperty(StudyNode.SOURCE, study.getSource());
            if (StringUtils.isNotBlank(study.getCitation())) {
                node.setProperty(StudyNode.CITATION, study.getCitation());
            }
            if (StringUtils.isNotBlank(study.getDOI())) {
                node.setProperty(StudyNode.DOI, study.getDOI());
            }
            if (StringUtils.isNotBlank(study.getExternalId())) {
                node.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, study.getExternalId());
            }
            node.setProperty(StudyNode.TITLE, study.getTitle());
            node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, interactionCounter.getCount() / 2);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, ids.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, 1);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, 1);
            getGraphDb().index().forNodes("reports").add(node, StudyNode.TITLE, study.getTitle());
            getGraphDb().index().forNodes("reports").add(node, StudyNode.SOURCE, study.getTitle());
            tx.success();
        } finally {
            tx.finish();
        }
    }

    void generateReportForStudySources() {
        final Set<String> sources = new HashSet<String>();
        NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
            @Override
            public void onStudy(StudyNode study) {
                sources.add(study.getSource());
            }
        });

        for (final String source : sources) {
            final Set<Long> distinctTaxonIds = new HashSet<Long>();
            final Counter counter = new Counter();
            final Counter studyCounter = new Counter();

            NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
                @Override
                public void onStudy(StudyNode study) {
                    if (StringUtils.equals(study.getSource(), source)) {
                        Iterable<Relationship> specimens = study.getSpecimens();
                        countInteractionsAndTaxa(specimens, distinctTaxonIds, counter);
                        studyCounter.count();
                    }

                }
            });

            Transaction tx = getGraphDb().beginTx();
            try {
                final Node node = getGraphDb().createNode();
                node.setProperty(StudyNode.SOURCE, source);
                node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, 1);

                getGraphDb().index().forNodes("reports").add(node, StudyNode.SOURCE, source);
                tx.success();
            } finally {
                tx.finish();
            }
        }


    }

    void generateReportForCollection() {


        final Set<Long> distinctTaxonIds = new HashSet<Long>();
        final Counter counter = new Counter();
        final Counter studyCounter = new Counter();
        final Set<String> distinctSources = new HashSet<String>();

        NodeUtil.findStudies(getGraphDb(), new StudyNodeListener() {
            @Override
            public void onStudy(StudyNode study) {
                Iterable<Relationship> specimens = study.getSpecimens();
                countInteractionsAndTaxa(specimens, distinctTaxonIds, counter);
                studyCounter.count();
                distinctSources.add(study.getSource());

            }
        });

        Transaction tx = getGraphDb().beginTx();
        try {
            final Node node = getGraphDb().createNode();
            node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES, distinctSources.size());
            getGraphDb().index().forNodes("reports").add(node, PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            tx.success();
        } finally {
            tx.finish();
        }


    }


    private void countInteractionsAndTaxa(Iterable<Relationship> specimens, Set<Long> ids, Counter interactionCounter) {
        for (Relationship specimen : specimens) {
            Iterable<Relationship> relationships = specimen.getEndNode().getRelationships();
            for (Relationship relationship : relationships) {
                InteractType[] types = InteractType.values();
                for (InteractType type : types) {
                    if (relationship.isType(type) && !relationship.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                        interactionCounter.count();
                        break;
                    }
                }
            }
            Relationship classifiedAs = specimen.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
            if (classifiedAs != null) {
                Node taxonNode = classifiedAs.getEndNode();
                ids.add(taxonNode.getId());
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
