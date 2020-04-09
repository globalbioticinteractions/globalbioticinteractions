package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;

public abstract class GraphService {

    private static GraphDatabaseService graphDb;

    public static GraphDatabaseService getGraphService(String baseDir) {
        if (graphDb == null) {
            graphDb = startNeo4j(baseDir);
        }
        return graphDb;
    }

    public static GraphDatabaseService startNeo4j(String baseDir) {
        String storePath = baseDir + "graph.db";
        System.out.println("neo4j starting using [" + storePath + "]...");

        final GraphDatabaseService graphService = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(storePath))
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "150M size")
                .setConfig(GraphDatabaseSettings.logical_log_rotation_threshold, "50M")
                .setConfig(GraphDatabaseSettings.check_point_interval_tx, "1000")
                .newGraphDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("neo4j stopping...");
                graphService.shutdown();
                System.out.println("neo4j stopped.");
            }
        });
        System.out.println("neo4j started (" + storePath + ").");
        return graphService;
    }

}