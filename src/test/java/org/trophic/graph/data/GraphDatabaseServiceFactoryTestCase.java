package org.trophic.graph.data;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.IOException;

public abstract class GraphDatabaseServiceFactoryTestCase {

    private GraphDatabaseService graphDb;

    @Before
    public void startGraphDb() throws IOException {
        graphDb = new org.neo4j.test.ImpermanentGraphDatabase();
    }

    @After
    public void shutdownGraphDb() {
        graphDb.shutdown();
    }

}
