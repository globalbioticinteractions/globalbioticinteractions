package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public abstract class GraphService {

    private static GraphDatabaseService graphService;
    private static String storeDir = "graph.db";

    public static GraphDatabaseService getGraphService(String baseDir) {
        if (graphService == null) {
            graphService = startNeo4j(baseDir);
        }
        return graphService;
    }

    public static GraphDatabaseService startNeo4j(String baseDir) {
        System.out.println("neo4j starting...");

        graphService = new EmbeddedGraphDatabase(baseDir + storeDir, MapUtil.stringMap("keep_logical_logs", "1M size"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("neo4j stopping...");
                graphService.shutdown();
                System.out.println("neo4j stopped.");
            }
        });
        System.out.println("neo4j started (" + ((EmbeddedGraphDatabase) graphService).getStoreDir() + ").");
        return graphService;
    }

    public static void setStoreDir(String storeDir) {
        GraphService.storeDir = storeDir;
    }
}