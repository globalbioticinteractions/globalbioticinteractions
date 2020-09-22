package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;

public class GraphServiceFactoryProxy implements GraphServiceFactory {

    private final GraphDatabaseService service;

    public GraphServiceFactoryProxy(GraphDatabaseService service) {
        this.service = service;
    }

    @Override
    public GraphDatabaseService getGraphService() {
        return this.service;
    }

    @Override
    public void clear() {
        service.shutdown();
    }
}