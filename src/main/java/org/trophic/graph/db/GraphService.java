package org.trophic.graph.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public abstract class GraphService {

    private static GraphDatabaseService graphService;
    private static String storeDir = "./data";

    public static GraphDatabaseService getGraphService(){
        if (graphService == null)
            graphService = startNeo4j();
        return graphService;
    }

    private static GraphDatabaseService startNeo4j() {
        System.out.println("neo4j starting...");

        graphService = new EmbeddedGraphDatabase(storeDir);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("neo4j stopping...");
                graphService.shutdown();
                System.out.println("neo4j stopped.");
            }
        });
        System.out.println("neo4j started (" + ((EmbeddedGraphDatabase)graphService).getStoreDir() + ").");
        return graphService;
    }

    public static void setStoreDir(String storeDir) {
        GraphService.storeDir = storeDir;
    }
}