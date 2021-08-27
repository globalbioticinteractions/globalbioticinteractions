package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.TransientTransactionFailureException;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;

public class GraphDatabaseServiceBatchingTransactionsTest extends GraphDBTestCase {

    @Test
    public void batchTransactionsSuccess() {
        GraphDatabaseServiceBatchingTransactions graphDb = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);

        Transaction tx1 = graphDb.beginTx();
        tx1.success();
        tx1.close();

        Transaction tx2 = graphDb.beginTx();
        tx2.success();
        tx2.close();

        Transaction tx3 = graphDb.beginTx();
        tx3.success();
        tx3.close();

        assertThat(tx1, is(equalTo(tx2)));
        assertThat(tx1, is(not(equalTo(tx3))));

    }

    @Test
    public void batchTransactionFailFirst() {
        GraphDatabaseServiceBatchingTransactions graphDb = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);

        Transaction tx1 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx1.failure();
        tx1.close();

        Transaction tx2 = graphDb.beginTx();
        tx2.success();
        tx2.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertFalse(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }

        Transaction tx3 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx3.success();
        tx3.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertTrue(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }


        assertThat(tx1, is(equalTo(tx2)));
        assertThat(tx1, is(not(equalTo(tx3))));

    }

    @Test
    public void batchTransactionCreatedIncompleteBatch() throws Exception {
        GraphDatabaseServiceBatchingTransactions graphDb
                = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertFalse(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }

        Transaction tx1 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx1.success();
        tx1.close();

        assertTrue(graphDb.hasIncompleteBatch());

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertTrue(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }

        graphDb.close();
        assertFalse(graphDb.hasIncompleteBatch());

        Transaction tx2 = graphDb.beginTx();
        tx2.success();
        tx2.close();

        assertThat(tx1, is(not(equalTo(tx2))));

    }

    @Test(expected = TransactionFailureException.class)
    public void batchTransactionFailLast() {
        GraphDatabaseServiceBatchingTransactions graphDb = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);

        Transaction tx1 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx1.success();
        tx1.close();

        Transaction tx2 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx2.failure();
        tx2.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertThat(getGraphDb().findNodes(Label.label("foo")).stream().count(),
                    is(0));
            tx.success();
        }

        Transaction tx3 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx3.success();
        tx3.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertTrue(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }


        assertThat(tx1, is(equalTo(tx2)));
        assertThat(tx1, is(not(equalTo(tx3))));

    }


    @Test
    public void batchTransactionTerminateFirst() {
        GraphDatabaseServiceBatchingTransactions graphDb = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);

        Transaction tx1 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx1.terminate();
        tx1.close();

        Transaction tx2 = graphDb.beginTx();
        tx2.success();
        tx2.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertFalse(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }

        Transaction tx3 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx3.success();
        tx3.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertTrue(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }


        assertThat(tx1, is(not(equalTo(tx2))));
        assertThat(tx2, is(equalTo(tx3)));

    }

    @Test(expected = TransientTransactionFailureException.class)
    public void batchTransactionTerminateLast() {
        GraphDatabaseServiceBatchingTransactions graphDb = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);

        Transaction tx1 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx1.success();
        tx1.close();

        Transaction tx2 = graphDb.beginTx();
        tx2.terminate();
        tx2.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertFalse(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }

        Transaction tx3 = graphDb.beginTx();
        graphDb.createNode(Label.label("foo"));
        tx3.success();
        tx3.close();

        try( Transaction tx = getGraphDb().beginTx() ) {
            assertTrue(getGraphDb().findNodes(Label.label("foo")).hasNext());
            tx.success();
        }


        assertThat(tx1, is(equalTo(tx2)));
        assertThat(tx1, is(not(equalTo(tx3))));

    }

}