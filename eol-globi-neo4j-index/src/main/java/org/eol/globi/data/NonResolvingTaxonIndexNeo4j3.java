package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.NonResolvingTaxonIndexNoTxNeo4j3;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class NonResolvingTaxonIndexNeo4j3 extends NonResolvingTaxonIndexNoTxNeo4j3 {

    public NonResolvingTaxonIndexNeo4j3(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    public Taxon getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        try (Transaction tx = getGraphDbService().beginTx()) {
            Taxon orCreateTaxon = super.getOrCreateTaxon(taxon);
            tx.success();
            return orCreateTaxon;
        }
    }

    @Override
    public Taxon findTaxonById(String externalId) {
        try (Transaction tx = getGraphDbService().beginTx()) {
            Taxon taxonById = super.findTaxonById(externalId);
            tx.success();
            return taxonById;
        }
    }

    @Override
    public Taxon findTaxonByName(String name) throws NodeFactoryException {
        try (Transaction tx = getGraphDbService().beginTx()) {
            Taxon taxonById = super.findTaxonByName(name);
            tx.success();
            return taxonById;
        }
    }
}
