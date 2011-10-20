package org.trophic.graph.service;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.IOException;

public class GraphDatabaseServiceFactoryTest {


    @Test
    public void getTestInstance() throws IOException {
        final GraphDatabaseService graphDb = new org.neo4j.test.ImpermanentGraphDatabase();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
        graphDb.shutdown();

    }

    @Test
    public void getInstance() throws IOException {
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
