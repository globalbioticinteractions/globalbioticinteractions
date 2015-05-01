package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.StudyListener;
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
        NodeUtil.findStudies(getGraphDb(), new StudyListener() {
            @Override
            public void onStudy(Study study) {
                generateReportForStudy(study);
            }
        });

    }

    protected void generateReportForStudy(Study study) {
        Set<Long> ids = new HashSet<Long>();
        generateReportForStudy(study, ids, new Counter());
    }

    protected void generateReportForStudy(Study study, Set<Long> ids, Counter interactionCounter) {
        Iterable<Relationship> specimens = study.getSpecimens();
        countInteractionsAndTaxa(specimens, ids, interactionCounter);

        Transaction tx = getGraphDb().beginTx();
        try {
            Node node = getGraphDb().createNode();
            node.setProperty(Study.SOURCE, study.getSource());
            if (StringUtils.isNotBlank(study.getCitation())) {
                node.setProperty(Study.CITATION, study.getCitation());
            }
            node.setProperty(Study.TITLE, study.getTitle());
            node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, interactionCounter.getCount() / 2);
            node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, ids.size());
            getGraphDb().index().forNodes("reports").add(node, Study.TITLE, study.getTitle());
            tx.success();
        } finally {
            tx.finish();
        }
    }

    void generateReportForStudySources() {
        final Set<String> sources = new HashSet<String>();
        NodeUtil.findStudies(getGraphDb(), new StudyListener() {
            @Override
            public void onStudy(Study study) {
                sources.add(study.getSource());
            }
        });

        for (final String source : sources) {
            final Set<Long> distinctTaxonIds = new HashSet<Long>();
            final Counter counter = new Counter();
            final Counter studyCounter = new Counter();

            NodeUtil.findStudies(getGraphDb(), new StudyListener() {
                @Override
                public void onStudy(Study study) {
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
                node.setProperty(Study.SOURCE, source);
                node.setProperty(PropertyAndValueDictionary.COLLECTION, GLOBI_COLLECTION_NAME);
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS, counter.getCount() / 2);
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA, distinctTaxonIds.size());
                node.setProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES, studyCounter.getCount());
                getGraphDb().index().forNodes("reports").add(node, Study.SOURCE, source);
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

        NodeUtil.findStudies(getGraphDb(), new StudyListener() {
            @Override
            public void onStudy(Study study) {
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
            Node taxonNode = classifiedAs.getEndNode();
            ids.add(taxonNode.getId());
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
