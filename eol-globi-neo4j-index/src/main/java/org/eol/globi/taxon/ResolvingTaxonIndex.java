package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;

public class ResolvingTaxonIndex extends NonResolvingTaxonIndex {
    private PropertyEnricher enricher;
    private boolean indexResolvedOnly;

    public ResolvingTaxonIndex(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(graphDbService);
        this.enricher = enricher;
    }

    @Override
    public TaxonNode getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        if (StringUtils.isBlank(taxon.getExternalId()) && StringUtils.length(taxon.getName()) < 3) {
            throw new NodeFactoryException("taxon name [" + taxon.getName() + "] too short and no externalId is provided");
        }
        TaxonNode taxonNode = findTaxon(taxon);
        return taxonNode == null ? createTaxon(taxon) : taxonNode;
    }

    private TaxonNode createTaxon(final Taxon origTaxon) throws NodeFactoryException {
        Taxon taxon = TaxonUtil.copy(origTaxon);
        return resolveAndIndex(origTaxon, taxon);
    }

    private TaxonNode resolveAndIndex(Taxon origTaxon, Taxon taxon) throws NodeFactoryException {
        TaxonNode indexedTaxon = findTaxon(taxon);
        while (indexedTaxon == null) {
            try {
                taxon = TaxonUtil.enrich(enricher, taxon);
            } catch (PropertyEnricherException e) {
                throw new NodeFactoryException("failed to enrich taxon with name [" + taxon.getName() + "]", e);
            }
            indexedTaxon = findTaxon(taxon);
            if (indexedTaxon == null) {
                if (TaxonUtil.isResolved(taxon)) {
                    indexedTaxon = createAndIndexTaxon(origTaxon, taxon);
                } else {
                    String truncatedName = NodeUtil.truncateTaxonName(taxon.getName());
                    if (StringUtils.equals(truncatedName, taxon.getName())) {
                        if (isIndexResolvedOnly()) {
                            break;
                        } else {
                            indexedTaxon = addNoMatchTaxon(origTaxon);
                        }
                    } else {
                        taxon = new TaxonImpl();
                        taxon.setName(truncatedName);
                        indexedTaxon = findTaxonByName(taxon.getName());
                    }
                }
            }
        }
        return indexedTaxon;
    }

    public void setEnricher(PropertyEnricher enricher) {
        this.enricher = enricher;
    }

    public void setIndexResolvedTaxaOnly(boolean indexResolvedOnly) {
        this.indexResolvedOnly = indexResolvedOnly;
    }

    public boolean isIndexResolvedOnly() {
        return indexResolvedOnly;
    }
}
