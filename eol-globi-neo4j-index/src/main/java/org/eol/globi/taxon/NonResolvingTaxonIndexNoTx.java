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
                : findTaxonById(taxon.getExternalId(), taxon);

        if (taxonFound == null
                && taxon != null
                && !StringUtils.isBlank(taxon.getName())) {
            taxonFound = findTaxonByName(taxon.getName(), taxon);
        }

        if (taxonFound == null) {
            try(Transaction transaction = getGraphDbService().beginTx()) {
                taxonFound = new TaxonNode(transaction.createNode(NodeLabel.Taxon_Verbatim));
                TaxonUtil.copy(taxon, taxonFound);
                transaction.commit();
            }
        }
        return taxonFound;
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        return findTaxonByName(name, null);
   }

    @Override
    public TaxonNode findTaxonByName(String name, Taxon taxonContext) throws NodeFactoryException {
        return findTaxonOrRelated(PropertyAndValueDictionary.NAME, name, getGraphDbService());
    }

    @Override
    public TaxonNode findTaxonById(String externalId) {
        return findTaxonById(externalId, null);
    }
    @Override
    public TaxonNode findTaxonById(String externalId, Taxon taxonContext) {
        return findTaxonOrRelated(PropertyAndValueDictionary.EXTERNAL_ID, externalId, getGraphDbService());
    }

    public GraphDatabaseService getGraphDbService() {
        return graphDb;
    }
}
