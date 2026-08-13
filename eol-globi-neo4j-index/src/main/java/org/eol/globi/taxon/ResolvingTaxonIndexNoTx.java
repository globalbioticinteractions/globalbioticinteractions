package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.UnlikelyTaxonNameException;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResolvingTaxonIndexNoTx implements ResolvingTaxonIndex {

    private final PropertyEnricher enricher;
    private final Transaction tx;

    private boolean skipHomonymMatches;
    private boolean indexResolvedOnly;

    public static final Pattern POSSIBLE_SHORT_NAME_PATTERN = Pattern.compile("[A-Z][a-z]");

    public ResolvingTaxonIndexNoTx(PropertyEnricher enricher, Transaction tx) {
        this.enricher = enricher;
        this.tx = tx;
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        return findTaxonByName(name, null);
    }

    @Override
    public TaxonNode findTaxonByName(String name, Taxon taxonContext) throws NodeFactoryException {
        return TaxonUtil.isEmptyValue(name)
                ? null
                : TaxonFinderUtil.findTaxonOrRelated(PropertyAndValueDictionary.NAME, name, taxonContext, getTransaction());
    }

    @Override
    public Taxon findTaxonById(String externalId) {
        return null;
    }

    @Override
    public TaxonNode findTaxonById(String externalId, Taxon taxonContext) {
        Transaction tx = getTransaction();
        return TaxonUtil.isEmptyValue(externalId)
                ? null
                : TaxonFinderUtil.findTaxonOrRelated(PropertyAndValueDictionary.EXTERNAL_ID, externalId, taxonContext, tx);
    }

    private Transaction getTransaction() {
        return tx;
    }

    @Override
    public Taxon getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        if (StringUtils.isBlank(taxon.getExternalId()) && StringUtils.length(taxon.getName()) < 3) {
            if (!POSSIBLE_SHORT_NAME_PATTERN.matcher(taxon.getName()).matches()) {
                throw new UnlikelyTaxonNameException("taxon name [" + taxon.getName() + "] is a short and unlikely taxonomic name, and no externalId is provided");
            }
        }

        Taxon taxonFound = findTaxonById(taxon.getExternalId(), taxon);

        if (taxonFound == null) {
            taxonFound = findTaxonByName(taxon.getName(), taxon);
        }

        if (taxonFound == null) {
            try {
                List<Map<String, String>> taxonResolved = enricher.enrichAllMatches(TaxonUtil.taxonToMap(taxon));

                List<TaxonNode> matchCandidates = (taxonResolved == null ? new ArrayList<Map<String, String>>() : taxonResolved)
                        .stream()
                        .filter(t -> !indexResolvedOnly || TaxonUtil.isResolved(t))
                        .map(TaxonUtil::mapToTaxon)
                        .filter(t -> includeAfterHomonymCheck(taxon, t))
                        .filter(t -> !TaxonUtil.hasLiteratureReference(t))
                        .map(r -> taxonNodeFor(r, NodeLabel.Taxon))
                        .collect(Collectors.toList());

                if (matchCandidates.isEmpty()) {
                    TaxonNode noMatch = createNoMatch(taxon);
                    taxonFound = indexResolvedOnly ? null : noMatch;
                } else {
                    TaxonNode primary = matchCandidates.get(0);
                    matchCandidates.stream().skip(1)
                            .forEach(n -> {
                                n.getUnderlyingNode().createRelationshipTo(primary.getUnderlyingNode(), NodeUtil.asNeo4j(RelTypes.SAME_AS));
                            });
                    taxonFound = primary;
                }
            } catch (PropertyEnricherException e) {
                // ignore
            }

        }
        return taxonFound;
    }

    private boolean includeAfterHomonymCheck(Taxon taxon, Taxon t) {
        return !(skipHomonymMatches && TaxonUtil.likelyHomonym(t, taxon));
    }

    private TaxonNode taxonNodeFor(Taxon r, NodeLabel nodeLabel) {
        TaxonNode t = new TaxonNode(getTransaction().createNode(nodeLabel));
        TaxonUtil.copy(r, t);
        return t;
    }

    private TaxonNode createNoMatch(Taxon taxon) {
        return taxonNodeFor(TaxonUtil.copyNoMatchTaxon(taxon), NodeLabel.Taxon_Resolved);
    }


    @Override
    public void setIndexResolvedTaxaOnly(boolean indexResolvedOnly) {
        this.indexResolvedOnly = indexResolvedOnly;
    }

    @Override
    public boolean isIndexResolvedOnly() {
        return indexResolvedOnly;
    }

    public void skipHomonymMatches(boolean skipHomonymMatches) {
        this.skipHomonymMatches = skipHomonymMatches;
    }


}
