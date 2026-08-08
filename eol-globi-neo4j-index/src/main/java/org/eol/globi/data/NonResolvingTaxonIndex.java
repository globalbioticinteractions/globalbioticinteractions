package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.NonResolvingTaxonIndexNoTx;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class NonResolvingTaxonIndex extends NonResolvingTaxonIndexNoTx {

    public NonResolvingTaxonIndex(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    public Taxon getOrCreateTaxon( Taxon taxon) throws NodeFactoryException {
        Taxon created = null;
        if (taxon != null)  {
            try (Transaction tx = getGraphDbService().beginTx()) {
                created = super.getOrCreateTaxon(taxon);
                tx.commit();
            }
        }
        return created;
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

    public Taxon findTaxon(Taxon taxon1) {
        return null;
    }

    public void skipHomonymMatches(boolean b) {

    }
}
