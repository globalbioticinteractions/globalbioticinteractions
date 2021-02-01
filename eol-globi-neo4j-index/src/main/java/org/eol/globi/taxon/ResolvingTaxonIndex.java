package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.List;
import java.util.Map;

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
        List<Map<String, String>> taxonMatches;
        while (indexedTaxon == null) {
            try {
                taxonMatches = enricher.enrichAllMatches(TaxonUtil.taxonToMap(taxon));
            } catch (PropertyEnricherException e) {
                throw new NodeFactoryException("failed to enrichFirstMatch taxon with name [" + taxon.getName() + "]", e);
            }
            if (taxonMatches != null && taxonMatches.size() > 0) {
                indexedTaxon = indexFirstAndConnectRemaining(taxonMatches, origTaxon);
            }

            if (indexedTaxon == null) {
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
        return indexedTaxon;
    }

    private TaxonNode indexFirstAndConnectRemaining(List<Map<String, String>> taxonMatches, Taxon origTaxon) throws NodeFactoryException {
        Taxon primaryTaxon = selectPrimaryTaxon(taxonMatches, origTaxon);
        return indexAndConnect(taxonMatches, origTaxon, primaryTaxon);
    }

    private Taxon selectPrimaryTaxon(List<Map<String, String>> taxonMatches, Taxon origTaxon) {
        Taxon primary = null;
        for (Map<String, String> taxonMatch : taxonMatches) {
            Taxon matchedTaxon = TaxonUtil.mapToTaxon(taxonMatch);
            if (!TaxonUtil.likelyHomonym(origTaxon, matchedTaxon)) {
                primary = matchedTaxon;
            }
            if (primary != null && !TaxonUtil.hasLiteratureReference(primary)) {
                break;
            }
        }
        return primary;
    }

    private TaxonNode indexAndConnect(List<Map<String, String>> taxonMatches, Taxon origTaxon, Taxon primaryTaxon) throws NodeFactoryException {
        TaxonNode indexedTaxon = findTaxon(primaryTaxon);
        if (indexedTaxon == null) {
            if (TaxonUtil.isResolved(primaryTaxon)) {
                indexedTaxon = createAndIndexTaxon(origTaxon, primaryTaxon);

                for (Map<String, String> taxonMatch : taxonMatches) {
                    Taxon sameAsTaxon = TaxonUtil.mapToTaxon(taxonMatch);
                    if (!TaxonUtil.likelyHomonym(indexedTaxon, sameAsTaxon)) {
                        NodeUtil.connectTaxa(
                                sameAsTaxon,
                                indexedTaxon,
                                getGraphDbService(),
                                RelTypes.SAME_AS
                        );
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
