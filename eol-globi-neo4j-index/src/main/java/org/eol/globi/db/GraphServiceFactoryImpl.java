package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;

public class GraphServiceFactoryImpl implements GraphServiceFactory {

    private final String baseDir;
    private static GraphDatabaseService graphDb;

    public GraphServiceFactoryImpl(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public GraphDatabaseService getGraphService() {
        GraphServiceUtil.verifyState(graphDb);

        if (graphDb == null) {
            graphDb = startNeo4j(baseDir);
        }
        return graphDb;
    }

    @Override
    public void shutdown() {
        if (graphDb != null) {
            graphDb.shutdown();
            graphDb = null;
        }
    }

    private static GraphDatabaseService startNeo4j(String baseDir) {
        String storePath = baseDir + "graph.db";
        System.out.println("neo4j starting using [" + storePath + "]...");

        final GraphDatabaseService graphService = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(storePath))
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "150M size")
                .setConfig(GraphDatabaseSettings.logical_log_rotation_threshold, "50M")
                .setConfig(GraphDatabaseSettings.check_point_interval_tx, "100000")
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