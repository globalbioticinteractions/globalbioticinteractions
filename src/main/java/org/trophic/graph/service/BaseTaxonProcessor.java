package org.trophic.graph.service;

import org.neo4j.graphdb.GraphDatabaseService;

public abstract class BaseTaxonProcessor implements TaxonProcessor {
    protected GraphDatabaseService graphDbService;

    public BaseTaxonProcessor(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    public GraphDatabaseService getGraphDbService() {
        return graphDbService;
    }

}
