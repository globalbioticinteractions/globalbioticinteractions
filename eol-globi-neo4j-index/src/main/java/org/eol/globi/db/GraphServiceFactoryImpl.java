package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;

public class GraphServiceFactoryImpl implements GraphServiceFactory {

    private final String baseDir;

    public GraphServiceFactoryImpl(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public GraphDatabaseService getGraphService() {
        return GraphService.getGraphService(baseDir);
    }

    @Override
    public void shutdown() {
        final GraphDatabaseService graphService = getGraphService();
        graphService.shutdown();
    }
}