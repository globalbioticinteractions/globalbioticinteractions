package org.trophic.graph.service;

import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

public abstract class TaxonEnricher extends BaseTaxonProcessor  {

    public TaxonEnricher(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    public void process() throws IOException {
        enrichTaxonUsingMatch("taxon<-[:CLASSIFIED_AS]-specimen ");
    }

    protected abstract void enrichTaxonUsingMatch(String matchString) throws IOException;
}
