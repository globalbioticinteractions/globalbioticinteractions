package org.trophic.graph.service;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class GraphDatabaseServiceFactoryTest {


    @Test
    public void getInstance() {
        final GraphDatabaseService graphDb = new EmbeddedGraphDatabase("data");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
        graphDb.shutdown();

    }
}
