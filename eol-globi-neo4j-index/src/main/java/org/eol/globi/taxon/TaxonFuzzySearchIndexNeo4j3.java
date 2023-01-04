package org.eol.globi.taxon;

import org.neo4j.graphdb.GraphDatabaseService;

public class TaxonFuzzySearchIndexNeo4j3 extends TaxonFuzzySearchIndexNeo4j2 {
    public TaxonFuzzySearchIndexNeo4j3(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }
}
