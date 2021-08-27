package org.eol.globi.service;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GraphDatabaseServiceBatchingTransactions
        extends GraphDatabaseServiceProxy
        implements AutoCloseable {
    public static final int BATCH_SIZE_DEFAULT = 1000;

    private int batchSize = BATCH_SIZE_DEFAULT;
    private AtomicReference<Transaction> currentTransaction = new AtomicReference<>();
    private AtomicInteger timeToLive = new AtomicInteger(getBatchSize());

    public GraphDatabaseServiceBatchingTransactions(GraphDatabaseService graphDb) {
        super(graphDb);
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public Transaction beginTx() {
        Transaction currentTx = currentTransaction.get();
        if (timeToLive.get() == 0 || currentTx == null) {
            if (currentTx != null) {
                currentTx.close();
            }
            currentTransaction
                    .set(new TransactionProxy(getGraphDb().beginTx()));
            timeToLive.set(getBatchSize());
        }
        timeToLive.decrementAndGet();
        return currentTransaction.get();
    }

    @Override
    public void close() {
        Transaction currentTx = currentTransaction.get();
        if (currentTx != null) {
            currentTransaction.set(null);
            timeToLive.set(0);
            currentTx.close();
        }
    }

    boolean hasIncompleteBatch() {
        return currentTransaction.get() != null;
    }

    private class TransactionProxy implements Transaction {

        private final Transaction tx;

        TransactionProxy(Transaction tx) {
            this.tx = tx;
        }

        @Override
        public void terminate() {
            timeToLive.set(0);
            tx.terminate();
        }

        @Override
        public void failure() {
            timeToLive.set(0);
            tx.failure();
        }

        @Override
        public void success() {
            tx.success();
        }

        @Override
        public void close() {
            if (timeToLive.get() == 0) {
                tx.close();
            }
        }

        @Override
        public Lock acquireWriteLock(PropertyContainer entity) {
            return tx.acquireWriteLock(entity);
        }

        @Override
        public Lock acquireReadLock(PropertyContainer entity) {
            return tx.acquireReadLock(entity);
        }
    }
}
