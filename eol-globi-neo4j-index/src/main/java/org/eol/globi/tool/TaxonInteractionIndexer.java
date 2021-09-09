package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.eol.globi.util.NodeProcessorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.util.NodeUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Map;

public class TaxonInteractionIndexer implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonInteractionIndexer.class);
    private final GraphServiceFactory factory;

    TaxonInteractionIndexer(GraphServiceFactory factory) {
        this.factory = factory;
    }

    @Override
    public void index() {
        LOG.info("indexing interactions started...");
        indexInteractions(factory.getGraphService());
        LOG.info("indexing interactions complete.");
    }


    private void indexInteractions(GraphDatabaseService graphService) {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final Map<Fun.Tuple3<Long, String, Long>, Long> taxonInteractions = db
                .createTreeMap("ottIdMap")
                .make();

        collectTaxonInteractions(taxonInteractions, graphService);
        createTaxonInteractions(taxonInteractions, graphService);

        db.close();
    }

    private void createTaxonInteractions(Map<Fun.Tuple3<Long, String, Long>, Long> taxonInteractions, GraphDatabaseService graphService) {
        StopWatch watchForEntireRun = new StopWatch();
        watchForEntireRun.start();

        long count = 0;
        for (Fun.Tuple3<Long, String, Long> uniqueTaxonInteraction : taxonInteractions.keySet()) {
            final Node sourceTaxon = graphService.getNodeById(uniqueTaxonInteraction.a);
            final Node targetTaxon = graphService.getNodeById(uniqueTaxonInteraction.c);
            if (sourceTaxon != null && targetTaxon != null) {
                final InteractType relType = InteractType.valueOf(uniqueTaxonInteraction.b);
                final Long interactionCount = taxonInteractions.get(uniqueTaxonInteraction);
                createInteraction(sourceTaxon, targetTaxon, relType, false, interactionCount);
                createInteraction(targetTaxon, sourceTaxon, InteractType.inverseOf(relType), true, interactionCount);
            }
            count++;
        }

        watchForEntireRun.stop();
        LOG.info("created [" + count + "] taxon interactions in " + getProgressMsg(count, watchForEntireRun.getTime()));
    }

    private void createInteraction(Node sourceTaxon, Node targetTaxon, InteractType relType, boolean inverted, Long interactionCount) {
        final Relationship interactRel = sourceTaxon.createRelationshipTo(targetTaxon, NodeUtil.asNeo4j(relType));
        SpecimenNode.enrichWithInteractProps(relType, interactRel, inverted);
        interactRel.setProperty("count", interactionCount);
    }

    private void collectTaxonInteractions(
            Map<Fun.Tuple3<Long, String, Long>, Long> taxonInteractions,
            GraphDatabaseService graphService) {

        new NodeProcessorImpl(
                graphService,
                1000L,
                "name",
                "*",
                "taxons")
                .process(taxonNode -> onTaxonNode(taxonInteractions, taxonNode)
                , new TransactionPerBatch(graphService));

    }

    private void onTaxonNode(Map<Fun.Tuple3<Long, String, Long>, Long> taxonInteractions, Node sourceTaxon) {
        final Iterable<Relationship> classifiedAs = sourceTaxon.getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
        for (Relationship classifiedA : classifiedAs) {
            Node specimenNode = classifiedA.getStartNode();
            final Iterable<Relationship> interactions = specimenNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.values()));
            for (Relationship interaction : interactions) {
                final Iterable<Relationship> targetClassifications = interaction.getEndNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
                for (Relationship targetClassification : targetClassifications) {
                    final Node targetTaxonNode = targetClassification.getEndNode();
                    final Fun.Tuple3<Long, String, Long> interactionKey = new Fun.Tuple3<Long, String, Long>(sourceTaxon.getId(), interaction.getType().name(), targetTaxonNode.getId());
                    final Long distinctInteractions = taxonInteractions.get(interactionKey);
                    taxonInteractions.put(interactionKey, distinctInteractions == null ? 1L : (distinctInteractions + 1L));
                }
            }
        }
    }

    private static String getProgressMsg(long count, long duration) {
        return String.format("[%.2f] taxon/s over [%.2f] s", (float) count * 1000.0 / duration, duration / 1000.0);
    }

}
