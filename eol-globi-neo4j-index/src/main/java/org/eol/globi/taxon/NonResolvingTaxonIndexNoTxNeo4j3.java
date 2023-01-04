package org.eol.globi.taxon;

import org.neo4j.graphdb.GraphDatabaseService;

public class NonResolvingTaxonIndexNoTxNeo4j3 extends NonResolvingTaxonIndexNoTxNeo4j2 {

    public NonResolvingTaxonIndexNoTxNeo4j3(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }
}
