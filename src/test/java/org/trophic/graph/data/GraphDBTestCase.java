package org.trophic.graph.data;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.taxon.TaxonLookupService;

import java.io.IOException;

public abstract class GraphDBTestCase {

    private GraphDatabaseService graphDb;

    protected NodeFactory nodeFactory;

    @Before
    public void startGraphDb() throws IOException {
        graphDb = new org.neo4j.test.ImpermanentGraphDatabase();
        nodeFactory = new NodeFactory(graphDb, new TaxonLookupService() {
            @Override
            public long[] lookupTerms(String taxonName) throws IOException {
                return new long[0];
            }

            @Override
            public void destroy() {

            }
        });
    }

    @After
    public void shutdownGraphDb() {
        graphDb.shutdown();
    }

    protected GraphDatabaseService getGraphDb() {
        return graphDb;
    }

}
