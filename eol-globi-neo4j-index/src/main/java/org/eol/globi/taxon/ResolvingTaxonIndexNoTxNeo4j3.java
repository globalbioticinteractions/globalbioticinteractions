package org.eol.globi.taxon;

import org.eol.globi.service.PropertyEnricher;
import org.neo4j.graphdb.GraphDatabaseService;

public class ResolvingTaxonIndexNoTxNeo4j3 extends ResolvingTaxonIndexNoTxNeo4j2 {

    public ResolvingTaxonIndexNoTxNeo4j3(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(enricher, graphDbService);
    }
}
