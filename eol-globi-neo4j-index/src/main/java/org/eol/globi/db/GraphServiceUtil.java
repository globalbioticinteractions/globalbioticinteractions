package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;

public final class GraphServiceUtil {

    private static final long TIMEOUT_MS = 5000;

    public static void verifyState(GraphDatabaseService graphDb) {
        if (graphDb == null) {
            throw new RuntimeException("graphDb not found");
        } else if (!graphDb.isAvailable(TIMEOUT_MS)) {
            throw new RuntimeException("graphDb not ready for use in [" + TIMEOUT_MS + "] ms");
        }
    }
}