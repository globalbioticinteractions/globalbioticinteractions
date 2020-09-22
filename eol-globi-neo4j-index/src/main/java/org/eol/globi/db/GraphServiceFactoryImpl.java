package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;

public class GraphServiceFactoryImpl implements GraphServiceFactory {

    private final String baseDir;
    private static GraphDatabaseService graphDb;

    public GraphServiceFactoryImpl(String baseDir) {
        if (graphDb != null) {
            throw new IllegalStateException("only one graph service factory allowed, but another is already instantiated");
        }
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
    public void clear() {
        if (graphDb != null) {
            graphDb.shutdown();
            graphDb = null;
        }
    }

    private static GraphDatabaseService startNeo4j(String baseDir) {
        String storePath = baseDir + "graph.db";
        System.out.print("neo4j starting at [" + storePath + "]...");

        final GraphDatabaseService graphService = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(storePath))
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "150M size")
                .setConfig(GraphDatabaseSettings.logical_log_rotation_threshold, "50M")
                .setConfig(GraphDatabaseSettings.check_point_interval_tx, "100000")
                .newGraphDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (graphService.isAvailable(0)) {
                System.out.print("neo4j stopping...");
                graphService.shutdown();
                System.out.println("done.");
            }
        }));
        System.out.print("done");
        return graphService;
    }

}