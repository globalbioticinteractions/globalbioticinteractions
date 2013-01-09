package org.trophic.graph.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

import java.io.IOException;
import java.util.Iterator;

public class TaxonPrunerService extends BaseTaxonProcessor {
    private static final Log LOG = LogFactory.getLog(TaxonPrunerService.class);

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    private int maxBatchSize = 1000;

    public TaxonPrunerService(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    public void process() throws IOException {
        ExecutionEngine engine = new ExecutionEngine(getGraphDbService());

        ExecutionResult execute = engine.execute("START taxon = node:taxons('*:*') MATCH taxon<-[r?:CLASSIFIED_AS]-specimen WHERE r is null RETURN taxon, r");
        Iterator<Object> taxon = execute.columnAs("taxon");

        long total = 0;
        int count = 0;
        Transaction transaction = null;
        try {
            Index<Node> taxonsIndex = getGraphDbService().index().forNodes("taxons");

            while (taxon.hasNext()) {
                if (transaction != null && count > maxBatchSize) {
                    transaction.success();
                    transaction.finish();
                    count = 0;
                    transaction = null;
                }

                if (transaction == null) {
                    transaction = getGraphDbService().beginTx();
                }

                Node next = (Node) taxon.next();
                Iterable<Relationship> relationships = next.getRelationships();
                for (Relationship relationship : relationships) {
                    relationship.delete();
                }

                next.delete();

                taxonsIndex.remove(next);
                count++;
                total++;
            }
            if (transaction != null) {
                transaction.success();
            }
        } finally {
            LOG.info("Pruned [" + total + "] unused taxa.");
            if (transaction != null) {
                transaction.finish();
            }
        }
    }
}
