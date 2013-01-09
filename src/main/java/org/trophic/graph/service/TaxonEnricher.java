package org.trophic.graph.service;

import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

public abstract class TaxonEnricher extends BaseTaxonProcessor  {

    public TaxonEnricher(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    public void process() throws IOException {
        String predatorTaxons = "study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon ";
        enrichTaxonUsingMatch(predatorTaxons);
        String preyTaxons = "study-[:COLLECTED]->predator-[:ATE]->prey-[:CLASSIFIED_AS]->taxon ";
        enrichTaxonUsingMatch(preyTaxons);
    }

    protected abstract void enrichTaxonUsingMatch(String matchString) throws IOException;
}
