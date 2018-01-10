package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.MapUtil;

import java.util.Map;

public abstract class GraphService {

    public static final Map<String, String> CONFIG_DEFAULT = MapUtil.stringMap("keep_logical_logs", "0M size");
    private static GraphDatabaseService graphDb;


    public static GraphDatabaseService getGraphService(String baseDir) {
        return getGraphService(baseDir, CONFIG_DEFAULT);
    }

    public static GraphDatabaseService getGraphService(String baseDir, Map<String, String> config) {
        if (graphDb == null) {
            graphDb = startNeo4j(baseDir, config);
        }
        return graphDb;
    }

    public static GraphDatabaseService startNeo4j(String baseDir) {
        return startNeo4j(baseDir, CONFIG_DEFAULT);
    }

    public static GraphDatabaseService startNeo4j(String baseDir, Map<String, String> config) {
        String storePath = baseDir + "graph.db";
        System.out.println("neo4j starting using [" + storePath + "]...");

        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storePath);
        graphDatabaseBuilder.setConfig(config);
        final GraphDatabaseService graphService = graphDatabaseBuilder.newGraphDatabase();
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