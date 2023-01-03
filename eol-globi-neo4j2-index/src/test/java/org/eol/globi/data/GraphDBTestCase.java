package org.eol.globi.data;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class GraphDBTestCase extends GraphDBTestCaseAbstract {


    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        GraphDatabaseService graphDb = getGraphDb();
        try (Transaction tx = graphDb.beginTx()) {
            NodeFactoryNeo4j2 nodeFactoryNeo4j = new NodeFactoryNeo4j2(graphDb);
            nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
            nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
            tx.success();
            return nodeFactoryNeo4j;
        }
    }

    @Override
    public void afterGraphDBStart() {
        getGraphDb().beginTx();
    }

    @Override
    public void beforeGraphDbShutdown() {
        //transaction.success();
        //transaction.close();
    }


}
