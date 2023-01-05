package org.eol.globi.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import static org.eol.globi.taxon.ResolvingTaxonIndexNoTxNeo4j3.findTaxonOrRelated;

public class NonResolvingTaxonIndexNoTxNeo4j3 implements TaxonIndex {

    private final GraphDatabaseService graphDb;

    public NonResolvingTaxonIndexNoTxNeo4j3(GraphDatabaseService graphDbService) {
        this.graphDb = graphDbService;
    }

    @Override
    public Taxon getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        Taxon taxonFound = findTaxonById(taxon.getExternalId());

        if (taxonFound == null) {
            taxonFound = findTaxonByName(taxon.getName());
        }

        if (taxonFound == null) {
            taxonFound = new TaxonNode(getGraphDbService().createNode());
            TaxonUtil.copy(taxon, taxonFound);
        }
        return taxonFound;
    }

    @Override
    public Taxon findTaxonByName(String name) throws NodeFactoryException {
        return findTaxonOrRelated(PropertyAndValueDictionary.NAME, name, getGraphDbService());
    }

    @Override
    public Taxon findTaxonById(String externalId) {
        return findTaxonOrRelated(PropertyAndValueDictionary.EXTERNAL_ID, externalId, getGraphDbService());
    }

    public GraphDatabaseService getGraphDbService() {
        return graphDb;
    }
}
