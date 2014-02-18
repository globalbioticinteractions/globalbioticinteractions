package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.MapUtil;

public abstract class GraphService {

    private static GraphDatabaseService graphDb;

    public static GraphDatabaseService getGraphService(String baseDir) {
        if (graphDb == null) {
            graphDb = startNeo4j(baseDir);
        }
        return graphDb;
    }

    public static GraphDatabaseService startNeo4j(String baseDir) {
        System.out.println("neo4j starting...");

        String storePath = baseDir + "graph.db";
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storePath);
        graphDatabaseBuilder.setConfig(MapUtil.stringMap("keep_logical_logs", "1M size"));
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