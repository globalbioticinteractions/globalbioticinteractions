package org.trophic.graph.service;

import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

public abstract class BaseTaxonEnricher implements TaxonEnricher {
    protected GraphDatabaseService graphDbService;

    public BaseTaxonEnricher(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    @Override
    public void enrichTaxons() throws IOException {
        String predatorTaxons = "study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon ";
        enrichTaxonUsingMatch(predatorTaxons);
        String preyTaxons = "study-[:COLLECTED]->predator-[:ATE]->prey-[:CLASSIFIED_AS]->taxon ";
        enrichTaxonUsingMatch(preyTaxons);
    }

    protected abstract void enrichTaxonUsingMatch(String matchString) throws IOException;
}
