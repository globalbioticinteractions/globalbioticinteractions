package org.eol.globi.data;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResultTransformer;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class GraphDatabaseServiceProxy implements GraphDatabaseService {
    private final GraphDatabaseService graphDb;
    private final AtomicBoolean shouldStartNextBatch;
    private final AtomicReference<Transaction> tx = new AtomicReference<>(null);

    public GraphDatabaseServiceProxy(GraphDatabaseService graphDb, AtomicBoolean shouldStartNextBatch) {
        this.graphDb = graphDb;
        this.shouldStartNextBatch = shouldStartNextBatch;
    }

    @Override
    public boolean isAvailable() {
        return graphDb.isAvailable();
    }

    @Override
    public boolean isAvailable(long timeoutMillis) {
        return graphDb.isAvailable(timeoutMillis);
    }

    @Override
    public Transaction beginTx() {
        if (shouldStartNextBatch.get() || tx.get() == null) {
            Transaction previousTx = tx.getAndSet(new TransactionProxy(graphDb.beginTx(), shouldStartNextBatch));
            if (shouldStartNextBatch.get() && previousTx != null) {
                previousTx.commit();
                previousTx.close();
                shouldStartNextBatch.set(false);
            }
        }
        return tx.get();
    }

    @Override
    public Transaction beginTx(long timeout, TimeUnit unit) {
        if (shouldStartNextBatch.get() || tx.get() == null) {
            tx.set(new TransactionProxy(graphDb.beginTx(timeout, unit), shouldStartNextBatch));
        }
        return tx.get();
    }

    @Override
    public void executeTransactionally(String query) throws QueryExecutionException {
        graphDb.executeTransactionally(query);
    }

    @Override
    public void executeTransactionally(String query, Map<String, Object> parameters) throws QueryExecutionException {
        graphDb.executeTransactionally(query, parameters);
    }

    @Override
    public <T> T executeTransactionally(String query, Map<String, Object> parameters, ResultTransformer<T> resultTransformer) throws QueryExecutionException {
        return graphDb.executeTransactionally(query, parameters, resultTransformer);
    }

    @Override
    public <T> T executeTransactionally(String query, Map<String, Object> parameters, ResultTransformer<T> resultTransformer, Duration timeout) throws QueryExecutionException {
        return graphDb.executeTransactionally(query, parameters, resultTransformer);
    }

    @Override
    public String databaseName() {
        return graphDb.databaseName();
    }

}
