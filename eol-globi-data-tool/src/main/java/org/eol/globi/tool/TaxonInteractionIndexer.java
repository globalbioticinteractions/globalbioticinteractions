package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Set;

public class TaxonInteractionIndexer {
    private static final Log LOG = LogFactory.getLog(TaxonInteractionIndexer.class);

    private final GraphDatabaseService graphService;

    public TaxonInteractionIndexer(GraphDatabaseService graphService) {
        this.graphService = graphService;
    }

    public void index() {
        LOG.info("indexing interactions started...");
        indexInteractions();
        LOG.info("indexing interactions complete.");
    }

    public void indexInteractions() {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final Set<Fun.Tuple3<Long, String, Long>> taxonInteractions = db
                .createTreeSet("ottIdMap")
                .make();

        collectTaxonInteractions(taxonInteractions);
        createTaxonInteractions(taxonInteractions);

        db.close();
    }

    public void createTaxonInteractions(Set<Fun.Tuple3<Long, String, Long>> taxonInteractions) {
        StopWatch watchForEntireRun = new StopWatch();
        watchForEntireRun.start();

        long count = 0;
        Transaction tx = null;
        for (Fun.Tuple3<Long, String, Long> uniqueTaxonInteraction : taxonInteractions) {
            if (count % 1000 == 0) {
                finalizeTx(tx);
                tx = graphService.beginTx();
            }
            final Node sourceTaxon = graphService.getNodeById(uniqueTaxonInteraction.a);
            final Node targetTaxon = graphService.getNodeById(uniqueTaxonInteraction.c);
            if (sourceTaxon != null && targetTaxon != null) {
                sourceTaxon.createRelationshipTo(targetTaxon, InteractType.valueOf(uniqueTaxonInteraction.b));
            }
            count++;
        }
        finalizeTx(tx);

        watchForEntireRun.stop();
        LOG.info("created [" + count + "] taxon interactions in " + getProgressMsg(count, watchForEntireRun.getTime()));
    }

    public void finalizeTx(Transaction tx) {
        if (tx != null) {
            tx.success();
            tx.finish();
        }
    }

    public void collectTaxonInteractions(Set<Fun.Tuple3<Long, String, Long>> taxonInteractions) {
        StopWatch watchForEntireRun = new StopWatch();
        watchForEntireRun.start();
        StopWatch watchForBatch = new StopWatch();
        watchForBatch.start();
        long count = 0L;
        int batchSize = 1000;


        Index<Node> taxonIndex = graphService.index().forNodes("taxons");
        IndexHits<Node> taxa = taxonIndex.query("name", "*");
        for (Node sourceTaxon : taxa) {
            final Iterable<Relationship> classifiedAs = sourceTaxon.getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
            for (Relationship classifiedA : classifiedAs) {
                Node specimenNode = classifiedA.getStartNode();
                final Iterable<Relationship> interactions = specimenNode.getRelationships(Direction.OUTGOING, InteractType.values());
                for (Relationship interaction : interactions) {
                    final Iterable<Relationship> targetClassifications = interaction.getEndNode().getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
                    for (Relationship targetClassification : targetClassifications) {
                        final Node targetTaxonNode = targetClassification.getEndNode();
                        taxonInteractions.add(new Fun.Tuple3<Long, String, Long>(sourceTaxon.getId(), interaction.getType().name(), targetTaxonNode.getId()));
                        count++;
                    }
                }
            }
            if (count % batchSize == 0) {
                watchForBatch.stop();
                final long duration = watchForBatch.getTime();
                if (duration > 0) {
                    LOG.info("walked [" + batchSize + "] interactions in " + getProgressMsg(batchSize, duration));
                }
                watchForBatch.reset();
                watchForBatch.start();
            }
        }
        taxa.close();
        watchForEntireRun.stop();
        LOG.info("walked [" + count + "] interactions in " + getProgressMsg(count, watchForEntireRun.getTime()));
    }

    public static String getProgressMsg(long count, long duration) {
        return String.format("[%.2f] taxon/s over [%.2f] s", (float) count * 1000.0 / duration, duration / 1000.0);
    }
}
