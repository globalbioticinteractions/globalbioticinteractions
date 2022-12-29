package org.eol.globi.data;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class NodeFactoryNeo4j3Test extends NodeFactoryNeo4jTest {

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryNeo4j3 nodeFactoryNeo4j = new NodeFactoryNeo4j3(getGraphDb());
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }

    @Override
    protected void assertDataset(String citationKey) throws NodeFactoryException {
        assertFalse(getGraphDb()
                .execute("MATCH (ds:Dataset { namespace: 'some/namespace' }) RETURN ds")
                .hasNext());

        super.assertDataset(citationKey);
        assertTrue(getGraphDb()
                .execute("MATCH (ds:Dataset) WHERE ds.namespace = 'some/namespace' return ds")
                .hasNext());

    }

}
