package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.logging.slf4j.Slf4jLogProvider;

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
    public void close() {
        if (graphDb != null) {
            graphDb.shutdown();
            graphDb = null;
        }
    }

    private static GraphDatabaseService startNeo4j(String baseDir) {
        File storeDir = new File(baseDir, "graph.db");
        System.err.print("neo4j starting at [" + storeDir.getAbsolutePath() + "]...");

        final GraphDatabaseService graphService = new GraphDatabaseFactory()
                .setUserLogProvider(new Slf4jLogProvider())
                .newEmbeddedDatabaseBuilder(storeDir)
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "500M size")
                .setConfig(GraphDatabaseSettings.logical_log_rotation_threshold, "250M")
//                .setConfig(GraphDatabaseSettings.check_point_interval_tx, "100000")
                .newGraphDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print("neo4j stopping...");
            if (graphService.isAvailable(0)) {
                graphService.shutdown();
            }
            System.out.println("done.");
        }));
        System.out.println("done");
        return graphService;
    }

}