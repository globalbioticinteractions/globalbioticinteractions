package org.eol.globi.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class NonResolvingTaxonIndexNeo4j2 extends NonResolvingTaxonIndexNoTxNeo4j2 {


    public NonResolvingTaxonIndexNeo4j2(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    public TaxonNode getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode orCreateTaxon = super.getOrCreateTaxon(taxon);
            tx.success();
            return orCreateTaxon;
        }
    }

    @Override
    public TaxonNode findTaxonById(String externalId) {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode taxonById = super.findTaxonById(externalId);
            tx.success();
            return taxonById;
        }
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode taxonById = super.findTaxonByName(name);
            tx.success();
            return taxonById;
        }
    }

}
