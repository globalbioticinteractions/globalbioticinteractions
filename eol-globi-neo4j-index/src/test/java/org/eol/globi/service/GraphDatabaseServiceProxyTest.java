package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class GraphDatabaseServiceProxyTest extends GraphDBTestCase {

    @Test
    public void init() {
        Label reference1 = Label.label("Reference");
        assertNoneOfLabel(reference1);

        GraphDatabaseService graphDb = new GraphDatabaseServiceProxy(getGraphDb());

        try (Transaction tx = graphDb.beginTx()) {
            Node reference = graphDb.createNode(reference1);
            reference.setProperty("foo", "bar");
            tx.success();
        }
        assertAtLeastOneOfLabel(reference1, getGraphDb());
        assertAtLeastOneOfLabel(reference1, graphDb);
    }

    private void assertNoneOfLabel(Label reference1) {
        try (Transaction tx = getGraphDb().beginTx()) {
            assertFalse(getGraphDb().findNodes(reference1).hasNext());
            tx.success();
        }
    }

    private void assertAtLeastOneOfLabel(Label label, GraphDatabaseService graphDb) {
        try (Transaction tx = graphDb.beginTx()) {
            assertTrue(graphDb.findNodes(label).hasNext());
            tx.success();
        }
    }

}