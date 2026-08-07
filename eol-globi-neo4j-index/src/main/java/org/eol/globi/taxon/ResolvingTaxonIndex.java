package org.eol.globi.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class ResolvingTaxonIndex extends ResolvingTaxonIndexNoTx {

    public ResolvingTaxonIndex(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(enricher, graphDbService);
    }

    @Override
    public TaxonNode findTaxonById(String externalId) {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode taxonById = super.findTaxonById(externalId);
            tx.commit();
            return taxonById;
        }
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        try (Transaction tx = getGraphDbService().beginTx()) {
            TaxonNode taxonById = super.findTaxonByName(name);
            tx.commit();
            return taxonById;
        }
    }


    public void setEnricher(PropertyEnricher enricher) {

    }

    public void skipHomonymMatches(boolean skipHomonymMatches) {

    }
}
