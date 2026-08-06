package org.eol.globi.data;

import org.neo4j.graphdb.Transaction;

import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class NodeFactoryNeo4j3Test extends NodeFactoryNeo4jTest {

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryNeo4j3 nodeFactoryNeo4j = new NodeFactoryNeo4j3(getGraphDb(), getCacheDir());
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }

    @Override
    protected void assertDataset(String citationKey, String namespace) throws NodeFactoryException {
        try (Transaction transaction1 = getGraphDb()
                .beginTx()) {
            assertFalse(transaction1.execute("MATCH (ds:Dataset { namespace: '" + namespace + "' }) RETURN ds")
                    .hasNext());
        }

        super.assertDataset(citationKey, namespace);
        try (Transaction transaction = getGraphDb()
                .beginTx()) {
            assertTrue(transaction
                    .execute("MATCH (ds:Dataset) WHERE ds.namespace = '" + namespace + "' return ds")
                    .hasNext());
        }

    }

}
