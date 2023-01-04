package org.eol.globi.data;

import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.tool.NodeFactoryFactory;
import org.eol.globi.tool.NodeFactoryFactoryTransactingOnDatasetNeo4j2;
import org.eol.globi.tool.NodeFactoryFactoryTransactingOnDatasetNeo4j3;
import org.hamcrest.core.Is;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class GraphDBNeo4j2TestCase extends GraphDBTestCaseAbstract {


    private Neo4jIndexType schemaType = Neo4jIndexType.noSchema;

    protected void setNeo4jIndexType(Neo4jIndexType type) {
        this.schemaType = type;
    }

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {


        NodeFactoryFactory factoryFactory;
        if (Neo4jIndexType.noSchema.equals(schemaType)) {
            factoryFactory
                    = new NodeFactoryFactoryTransactingOnDatasetNeo4j2(new GraphServiceFactory() {
                @Override
                public GraphDatabaseService getGraphService() {
                    return getGraphDb();
                }

                @Override
                public void close() throws Exception {

                }
            });
        } else {
            factoryFactory
                    = new NodeFactoryFactoryTransactingOnDatasetNeo4j3(new GraphServiceFactory() {
                @Override
                public GraphDatabaseService getGraphService() {
                    return getGraphDb();
                }

                @Override
                public void close() throws Exception {

                }
            });

        }


        GraphDatabaseService graphDb = getGraphDb();
        try (Transaction tx = graphDb.beginTx()) {
            NodeFactory nodeFactoryNeo4j = factoryFactory.create(graphDb);
            assertThat(nodeFactoryNeo4j, Is.is(instanceOf(NodeFactoryNeo4j.class)));
            NodeFactoryNeo4j factory = (NodeFactoryNeo4j) nodeFactoryNeo4j;
            factory.setEnvoLookupService(getEnvoLookupService());
            factory.setTermLookupService(getTermLookupService());
            tx.success();
            return factory;
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
