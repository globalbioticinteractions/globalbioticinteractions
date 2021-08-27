package org.eol.globi.service;

import org.eol.globi.db.GraphServiceFactory;
import org.neo4j.graphdb.GraphDatabaseService;

public class GraphServiceBatchingFactory implements GraphServiceFactory {

    private final GraphServiceFactory graphService;
    private GraphDatabaseServiceBatchingTransactions graphDatabaseServiceBatchingTransactions;

    public GraphServiceBatchingFactory(GraphServiceFactory graphService) {
        this.graphService = graphService;
    }

    @Override
    public GraphDatabaseServiceBatchingTransactions getGraphService() {
        if (graphDatabaseServiceBatchingTransactions == null) {
            GraphDatabaseService graphService1 = graphService.getGraphService();
            graphDatabaseServiceBatchingTransactions = new GraphDatabaseServiceBatchingTransactions(graphService1);
        }
        return graphDatabaseServiceBatchingTransactions;
    }

    @Override
    public void close() {
        if (graphDatabaseServiceBatchingTransactions != null) {
            graphDatabaseServiceBatchingTransactions.close();
        }
        if (graphService != null) {
            try {
                graphService.close();
            } catch (Exception e) {
                throw new RuntimeException("failed to close graphdb", e);
            }
        }
    }
}
