package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import static org.eol.globi.taxon.ResolvingTaxonIndexNoTx.findTaxonOrRelated;

public class NonResolvingTaxonIndexNoTx implements TaxonIndex {

    private final GraphDatabaseService graphDb;

    public NonResolvingTaxonIndexNoTx(GraphDatabaseService graphDbService) {
        this.graphDb = graphDbService;
    }

    @Override
    public Taxon getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        Taxon taxonFound = taxon == null || StringUtils.isBlank(taxon.getExternalId())
                ? null
                : findTaxonById(taxon.getExternalId());

        if (taxonFound == null
                && taxon != null
                && !StringUtils.isBlank(taxon.getName())) {
            taxonFound = findTaxonByName(taxon.getName());
        }

        if (taxonFound == null) {
            try(Transaction transaction = getGraphDbService().beginTx()) {
                taxonFound = new TaxonNode(transaction.createNode(NodeLabel.Taxon));
                TaxonUtil.copy(taxon, taxonFound);
                transaction.commit();
            }
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
