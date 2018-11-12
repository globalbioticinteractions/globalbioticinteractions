package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryParser.QueryParser;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.QueryUtil;
import org.eol.globi.service.TaxonUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Map;

public class NonResolvingTaxonIndex implements TaxonIndex {
    private final GraphDatabaseService graphDbService;
    private final Index<Node> taxons;
    
    private static final String[] RANKS = new String[]{"kingdom", "phylum", "class", "order", "family", "genus", "species"};

    public NonResolvingTaxonIndex(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
        this.taxons = graphDbService.index().forNodes("taxons");
    }

    @Override
    public TaxonNode getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        TaxonNode taxonNode = findTaxon(taxon);
        if (taxonNode == null) {
            taxonNode = TaxonUtil.isResolved(taxon)
                    ? createAndIndexTaxon(taxon, taxon)
                    : addNoMatchTaxon(taxon);
        }
        return taxonNode == null ? createAndIndexTaxon(taxon, taxon) : taxonNode;
    }

    @Override
    public TaxonNode findTaxonById(String externalId) {
        return findTaxonByKey(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        return findTaxonByKey(PropertyAndValueDictionary.NAME, name);
    }

    private TaxonNode findTaxonByKey(String key, String value) {
        TaxonNode firstMatchingTaxon = null;
        if (StringUtils.isNotBlank(value)) {
            String query = key + ":\"" + QueryParser.escape(value) + "\"";
            IndexHits<Node> matchingTaxa = taxons.query(query);
            Node matchingTaxon;
            if (matchingTaxa.hasNext()) {
                matchingTaxon = matchingTaxa.next();
                if (matchingTaxon != null) {
                    firstMatchingTaxon = new TaxonNode(matchingTaxon);
                }
            }
            matchingTaxa.close();
        }
        return firstMatchingTaxon;
    }

    protected TaxonNode findTaxon(Taxon taxon) throws NodeFactoryException {
        String name = taxon.getName();
        String externalId = taxon.getExternalId();
        TaxonNode taxon1 = null;
        if (StringUtils.isBlank(externalId)) {
            if (StringUtils.length(name) > 1) {
                taxon1 = findTaxonByName(name);
            }
        } else {
            taxon1 = findTaxonById(externalId);
        }
        return taxon1;
    }

    private void indexOriginalNameForTaxon(String name, Taxon taxon, TaxonNode taxonNode) throws NodeFactoryException {
        if (!StringUtils.equals(taxon.getName(), name)) {
            if (isNonEmptyTaxonNameOrId(name)) {
                if (findTaxonByName(name) == null) {
                    indexTaxonByProperty(taxonNode, PropertyAndValueDictionary.NAME, name);
                }
            }
        }
    }

    protected boolean isNonEmptyTaxonNameOrId(String name) {
        return StringUtils.isNotBlank(name)
                && !StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, name)
                && !StringUtils.equals(PropertyAndValueDictionary.NO_NAME, name);
    }

    protected void indexOriginalExternalIdForTaxon(String externalId, Taxon taxon, TaxonNode taxonNode) throws NodeFactoryException {
        if (!StringUtils.equals(taxon.getExternalId(), externalId)) {
            if (isNonEmptyTaxonNameOrId(externalId) && findTaxonById(externalId) == null) {
                indexTaxonByProperty(taxonNode, PropertyAndValueDictionary.EXTERNAL_ID, externalId);
            }
        }
    }

    public IndexHits<Node> findCloseMatchesForTaxonName(String taxonName) {
        return QueryUtil.query(taxonName, PropertyAndValueDictionary.NAME, taxons);
    }

    private void indexTaxonByProperty(TaxonNode taxonNode, String propertyName, String propertyValue) {
        Transaction tx = null;
        try {
            tx = taxonNode.getUnderlyingNode().getGraphDatabase().beginTx();
            taxons.add(taxonNode.getUnderlyingNode(), propertyName, propertyValue);
            tx.success();
        } finally {
            if (tx != null) {
                tx.finish();
            }
        }
    }

    protected TaxonNode createAndIndexTaxon(Taxon origTaxon, Taxon taxon) throws NodeFactoryException {
        TaxonNode taxonNode;
        Transaction transaction = graphDbService.beginTx();
        try {
            Node node = graphDbService.createNode();
            taxonNode = new TaxonNode(node, taxon.getName());

            TaxonNode copiedTaxon = (TaxonNode) TaxonUtil.copy(taxon, taxonNode);
            if (isNonEmptyTaxonNameOrId(taxonNode.getName())) {
                for (String rank : RANKS) {
                    populateRankIds(taxon, node, rank);
                    populateRankNames(taxon, node, rank);
                }
            }
            addToIndeces(copiedTaxon, taxon.getName());

            indexOriginalNameForTaxon(origTaxon.getName(), taxon, taxonNode);
            indexOriginalExternalIdForTaxon(origTaxon.getExternalId(), taxon, taxonNode);

            transaction.success();
        } finally {
            transaction.finish();
        }
        return taxonNode;
    }

    private void populateRankNames(Taxon taxon, Node node, String rank) {
        Map<String, String> pathNameMap = TaxonUtil.toPathNameMap(taxon);
        String name = pathNameMap.get(rank);
        if (StringUtils.isNotBlank(name)) {
            node.setProperty(rank + "Name", name);
        }
    }

    private void populateRankIds(Taxon taxon, Node node, String rank) {
        Map<String, String> pathIdMap = TaxonUtil.toPathIdMap(taxon);
        String id = pathIdMap.get(rank);
        if (StringUtils.isNotBlank(id)) {
            node.setProperty(rank + "Id", id);
        }
    }


    private void addToIndeces(TaxonNode taxon, String indexedName) {
        if (isNonEmptyTaxonNameOrId(indexedName)) {
            taxons.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, indexedName);
        }

        String externalId = taxon.getExternalId();
        if (isNonEmptyTaxonNameOrId(externalId)) {
            taxons.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        }
    }

    protected TaxonNode addNoMatchTaxon(Taxon origTaxon) throws NodeFactoryException {
        Taxon noMatchTaxon = TaxonUtil.copy(origTaxon);
        noMatchTaxon.setName(isNonEmptyTaxonNameOrId(origTaxon.getName()) ? origTaxon.getName() : PropertyAndValueDictionary.NO_NAME);
        noMatchTaxon.setExternalId(isNonEmptyTaxonNameOrId(origTaxon.getExternalId()) ? origTaxon.getExternalId() : PropertyAndValueDictionary.NO_MATCH);
        return createAndIndexTaxon(origTaxon, noMatchTaxon);
    }


}
