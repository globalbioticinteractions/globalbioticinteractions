package org.eol.globi.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class ResolvingTaxonIndex extends ResolvingTaxonIndexNoTx {


    public ResolvingTaxonIndex(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(enricher, graphDbService);
    }

    @Override
    public TaxonNode findTaxonById(String externalId, Taxon taxonContext) {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode taxonById = super.findTaxonById(externalId, taxonContext);
            tx.commit();
            return taxonById;
        }
    }

    @Override
    public TaxonNode findTaxonByName(String name, Taxon taxonContext) throws NodeFactoryException {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode taxonById = super.findTaxonByName(name, taxonContext);
            tx.commit();
            return taxonById;
        }
    }
}
