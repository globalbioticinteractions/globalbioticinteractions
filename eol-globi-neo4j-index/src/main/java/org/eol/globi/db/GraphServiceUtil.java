package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;

public final class GraphServiceUtil {

    public static void verifyState(GraphDatabaseService graphDb) {
        if (graphDb != null && !graphDb.isAvailable(5000)) {
            throw new RuntimeException("graphDb not available");
        }
    }
}