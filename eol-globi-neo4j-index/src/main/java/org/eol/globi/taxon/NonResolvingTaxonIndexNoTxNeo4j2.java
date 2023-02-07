package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.QueryUtil;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtilNeo4j2;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Map;
import java.util.function.Predicate;

public class NonResolvingTaxonIndexNoTxNeo4j2 implements TaxonIndex {
    private final GraphDatabaseService graphDbService;
    private Index<Node> taxons = null;

    private static final String[] RANKS = new String[]{
            "kingdom",
            "phylum",
            "class",
            "order",
            "family",
            "genus",
            "subgenus",
            "species"
    };


    private boolean skipHomonymMatches = false;

    public NonResolvingTaxonIndexNoTxNeo4j2(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    @Override
    public TaxonNode getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        return taxon == null ? null : doGetOrIndexTaxon(taxon);
    }

    private TaxonNode doGetOrIndexTaxon(Taxon taxon) throws NodeFactoryException {
        TaxonNode taxonNode = findTaxon(taxon);
        if (taxonNode == null) {
            taxonNode = TaxonUtil.isResolved(taxon)
                    ? createAndIndexTaxon(taxon, taxon)
                    : addNoMatchTaxon(taxon);
        }
        return taxonNode == null
                ? createAndIndexTaxon(taxon, taxon)
                : taxonNode;
    }

    @Override
    public TaxonNode findTaxonById(String externalId) {
        return findTaxonByKey(PropertyAndValueDictionary.EXTERNAL_ID, externalId, alwaysMatch());
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        return findTaxonByKey(PropertyAndValueDictionary.NAME, name, alwaysMatch());
    }

    private TaxonNode findTaxonByKey(String key, String value, Predicate<Taxon> selector) {
        TaxonNode firstMatchingTaxon = null;
        if (StringUtils.isNotBlank(value)) {
            String query = key + ":\"" + QueryParser.escape(value) + "\"";
            IndexHits<Node> matchingTaxa = getTaxonIndex().query(query);
            Node matchingTaxon;
            while (matchingTaxa.hasNext()) {
                matchingTaxon = matchingTaxa.next();
                if (matchingTaxon != null) {
                    TaxonNode taxonCandidate = new TaxonNode(matchingTaxon);
                    if (selector.test(taxonCandidate)) {
                        firstMatchingTaxon = taxonCandidate;
                        break;
                    }
                }
            }
            matchingTaxa.close();
        }
        return firstMatchingTaxon;
    }

    private Index<Node> getTaxonIndex() {
        if (taxons == null) {
            taxons = NodeUtilNeo4j2.forNodes(graphDbService, "taxons");
        }
        return taxons;
    }

    TaxonNode findTaxon(Taxon taxon) throws NodeFactoryException {
        TaxonNode taxon1 = null;
        if (taxon != null) {
            String externalId = taxon.getExternalId();
            if (StringUtils.isBlank(externalId)) {
                String name = taxon.getName();
                if (StringUtils.length(name) > 1) {
                    taxon1 = findTaxonByKey(PropertyAndValueDictionary.NAME, name, getMatchSelectorFor(taxon));
                }
            } else {
                taxon1 = findTaxonByKey(PropertyAndValueDictionary.EXTERNAL_ID, externalId, getMatchSelectorFor(taxon));
            }
        }
        return taxon1;
    }

    protected Predicate<Taxon> getMatchSelectorFor(Taxon taxon) {
        return shouldSkipHomonymMatches()
                ? new ExcludeHomonyms(taxon)
                : alwaysMatch();
    }

    private static Predicate<Taxon> alwaysMatch() {
        return t -> true;
    }

    private boolean shouldSkipHomonymMatches() {
        return skipHomonymMatches;
    }

    public void skipHomonymMatches(boolean skipHomonymMatches) {
        this.skipHomonymMatches = skipHomonymMatches;
    }

    private void indexOriginalNameForTaxon(String name, TaxonNode taxonNode) throws NodeFactoryException {
        if (!StringUtils.equals(taxonNode.getName(), name)) {
            if (isNonEmptyTaxonNameOrId(name)) {
                if (findTaxonByName(name) == null) {
                    indexTaxonByProperty(taxonNode, PropertyAndValueDictionary.NAME, name);
                }
            }
        }
    }

    private boolean isNonEmptyTaxonNameOrId(String name) {
        return StringUtils.isNotBlank(name)
                && !StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, name)
                && !StringUtils.equals(PropertyAndValueDictionary.AMBIGUOUS_MATCH, name)
                && !StringUtils.equals(PropertyAndValueDictionary.NO_NAME, name);
    }

    private void indexOriginalExternalIdForTaxon(String externalId, TaxonNode taxonNode) throws NodeFactoryException {
        if (!StringUtils.equals(taxonNode.getExternalId(), externalId)) {
            if (isNonEmptyTaxonNameOrId(externalId) && findTaxonById(externalId) == null) {
                indexTaxonByProperty(taxonNode, PropertyAndValueDictionary.EXTERNAL_ID, externalId);
            }
        }
    }

    IndexHits<Node> findCloseMatchesForTaxonName(String taxonName) {
        return QueryUtil.query(taxonName, PropertyAndValueDictionary.NAME, getTaxonIndex());
    }

    private void indexTaxonByProperty(TaxonNode taxonNode, String propertyName, String propertyValue) throws NodeFactoryException {
        NodeFactoryNeo4j2.indexNonBlankKeyValue(getTaxonIndex(), taxonNode.getUnderlyingNode(), propertyName, propertyValue);
    }

    protected GraphDatabaseService getGraphDbService() {
        return graphDbService;
    }

    protected TaxonNode createAndIndexTaxon(Taxon provided, Taxon resolved) throws NodeFactoryException {
        TaxonNode resolvedNode;
        Node node = graphDbService.createNode();
        resolvedNode = new TaxonNode(node, resolved.getName());

        TaxonUtil.copy(resolved, resolvedNode);
        if (isNonEmptyTaxonNameOrId(resolvedNode.getName())) {
            final Map<String, String> pathIdMap1 = TaxonUtil.toPathNameMap(resolved, resolved.getPathIds());
            final Map<String, String> pathNameMap1 = TaxonUtil.toPathNameMap(resolved, resolved.getPath());
            for (String rank : RANKS) {
                populateRankIds(node, rank, pathIdMap1);
                populateRankNames(node, rank, pathNameMap1);
            }
        }
        indexTaxon(provided, resolvedNode);
        return resolvedNode;
    }

    protected void indexTaxon(Taxon provided, TaxonNode resolved) throws NodeFactoryException {
        addToIndeces(resolved, resolved.getName());
        indexOriginalNameForTaxon(provided.getName(), resolved);
        indexOriginalExternalIdForTaxon(provided.getExternalId(), resolved);
    }

    private void populateRankNames(Node node, String rank, Map<String, String> pathNameMap1) {
        String name = pathNameMap1.get(rank);
        if (StringUtils.isNotBlank(name)) {
            node.setProperty(rank + "Name", name);
        }
    }

    private void populateRankIds(Node node, String rank, Map<String, String> pathIdMap1) {
        String id = pathIdMap1.get(rank);
        if (StringUtils.isNotBlank(id)) {
            node.setProperty(rank + "Id", id);
        }
    }


    private void addToIndeces(TaxonNode taxon, String indexedName) throws NodeFactoryException {
        if (isNonEmptyTaxonNameOrId(indexedName)) {
            NodeFactoryNeo4j2.indexNonBlankKeyValue(getTaxonIndex(), taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, indexedName);
        }

        String externalId = taxon.getExternalId();
        if (isNonEmptyTaxonNameOrId(externalId)) {
            NodeFactoryNeo4j2.indexNonBlankKeyValue(getTaxonIndex(), taxon.getUnderlyingNode(), PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        }
    }

    protected TaxonNode addNoMatchTaxon(Taxon origTaxon) throws NodeFactoryException {
        Taxon noMatchTaxon = TaxonUtil.copy(origTaxon);

        noMatchTaxon.setName(isNonEmptyTaxonNameOrId(origTaxon.getName())
                ? origTaxon.getName()
                : PropertyAndValueDictionary.NO_NAME);

        noMatchTaxon.setExternalId(isNonEmptyTaxonNameOrId(origTaxon.getExternalId())
                ? origTaxon.getExternalId()
                : PropertyAndValueDictionary.NO_MATCH);
        return createAndIndexTaxon(origTaxon, noMatchTaxon);
    }


}
