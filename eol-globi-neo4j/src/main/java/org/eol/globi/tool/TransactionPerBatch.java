package org.eol.globi.tool;

import org.eol.globi.util.BatchListener;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionPerBatch implements BatchListener {
    private final static Logger LOG = LoggerFactory.getLogger(TransactionPerBatch.class);
    private final GraphDatabaseService graphDb;
    private Transaction tx;

    public TransactionPerBatch(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    @Override
    public void onStart() {
        onFinish();
        LOG.info("start transaction");
        tx = graphDb.beginTx();
    }

    @Override
    public void onFinish() {
        if (tx != null) {
            tx.success();
            tx.close();
            LOG.info("close transaction");

        }
    }
}
