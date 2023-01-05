package org.eol.globi.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class ResolvingTaxonIndexNeo4j3 extends ResolvingTaxonIndexNoTxNeo4j3 {

    public ResolvingTaxonIndexNeo4j3(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(enricher, graphDbService);
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
