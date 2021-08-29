package org.eol.globi.data;

import org.neo4j.graphdb.Transaction;

public class GraphDBTestCase extends GraphDBTestCaseAbstract {

    private Transaction transaction;


    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryNeo4j2 nodeFactoryNeo4j = new NodeFactoryNeo4j2(getGraphDb());
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }

    @Override
    public void afterGraphDBStart() {
        transaction = getGraphDb().beginTx();
    }

    @Override
    public void beforeGraphDbShutdown() {
        //transaction.success();
        //transaction.close();
    }


}
