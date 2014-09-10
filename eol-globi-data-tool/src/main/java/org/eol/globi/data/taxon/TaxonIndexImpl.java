package org.eol.globi.data.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryParser.QueryParser;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonMatchValidator;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class TaxonIndexImpl implements TaxonIndex {
    private final GraphDatabaseService graphDbService;
    private final Index<Node> taxons;
    private final Index<Node> taxonNameSuggestions;
    private CorrectionService corrector;
    private PropertyEnricher enricher;

    public TaxonIndexImpl(PropertyEnricher enricher, CorrectionService correctionService, GraphDatabaseService graphDbService) {
        this.enricher = enricher;
        this.corrector = correctionService;
        this.graphDbService = graphDbService;
        this.taxons = graphDbService.index().forNodes("taxons");
        this.taxonNameSuggestions = graphDbService.index().forNodes("taxonNameSuggestions");
    }

    @Override
    public TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        if (StringUtils.isBlank(externalId) && StringUtils.length(name) < 3) {
            throw new NodeFactoryException("taxon name [" + name + "] too short and no externalId is provided");
        }
        TaxonNode taxon = findTaxon(name, externalId);
        return taxon == null ? createTaxon(name, externalId, path) : taxon;
    }

    private TaxonNode findTaxon(String name, String externalId) throws NodeFactoryException {
        TaxonNode taxon = null;
        if (StringUtils.isBlank(externalId)) {
            if (StringUtils.length(name) > 1) {
                taxon = findTaxonByName(name);
            }
        } else {
            taxon = findTaxonById(externalId);
        }
        return taxon;
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

    private TaxonNode createTaxon(final String name, final String externalId, final String path) throws NodeFactoryException {
        Taxon taxon = new TaxonImpl();
        taxon.setName(corrector.correct(name));
        taxon.setExternalId(externalId);
        taxon.setPath(path);

        TaxonNode taxonNode = findTaxon(taxon.getName(), taxon.getExternalId());
        while (taxonNode == null) {
            try {
                taxon = TaxonUtil.enrich(enricher, taxon);
            } catch (PropertyEnricherException e) {
                throw new NodeFactoryException("failed to enrich taxon with name [" + taxon.getName() + "]", e);
            }
            taxonNode = findTaxon(taxon.getName(), taxon.getExternalId());
            if (taxonNode == null) {
                if (TaxonMatchValidator.hasMatch(taxon)) {
                    taxonNode = createAndIndexTaxon(taxon);
                } else {
                    String truncatedName = NodeUtil.truncateTaxonName(taxon.getName());
                    if (truncatedName == null || StringUtils.length(truncatedName) < 3) {
                        taxonNode = addNoMatchTaxon(externalId, path, name);
                    } else {
                        taxon = new TaxonImpl();
                        taxon.setName(truncatedName);
                        taxonNode = findTaxonByName(taxon.getName());
                    }
                }
            }
        }
        indexOriginalNameForTaxon(name, taxon, taxonNode);
        indexOriginalExternalIdForTaxon(externalId, taxon, taxonNode);
        return taxonNode;
    }

    private void indexOriginalNameForTaxon(String name, Taxon taxon, TaxonNode taxonNode) throws NodeFactoryException {
        if (!StringUtils.equals(taxon.getName(), name)) {
            if (StringUtils.isNotBlank(name) && !StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, name)) {
                if (findTaxonByName(name) == null) {
                    indexTaxonByProperty(taxonNode, PropertyAndValueDictionary.NAME, name);
                }
            }
        }
    }

    private void indexOriginalExternalIdForTaxon(String externalId, Taxon taxon, TaxonNode taxonNode) throws NodeFactoryException {
        if (!StringUtils.equals(taxon.getExternalId(), externalId)) {
            if (StringUtils.isNotBlank(externalId) && !StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, externalId)) {
                if (findTaxonById(externalId) == null) {
                    indexTaxonByProperty(taxonNode, PropertyAndValueDictionary.EXTERNAL_ID, externalId);
                }
            }
        }
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

    private TaxonNode addNoMatchTaxon(String externalId, String path, String originalName) throws NodeFactoryException {
        Taxon noMatchTaxon = new TaxonImpl();
        noMatchTaxon.setName(StringUtils.isBlank(originalName) ? PropertyAndValueDictionary.NO_MATCH : originalName);
        noMatchTaxon.setExternalId(StringUtils.isBlank(externalId) ? PropertyAndValueDictionary.NO_MATCH : externalId);
        noMatchTaxon.setPath(path);
        return createAndIndexTaxon(noMatchTaxon);
    }

    private TaxonNode createAndIndexTaxon(Taxon taxon) throws NodeFactoryException {
        TaxonNode taxonNode = null;
        Transaction transaction = graphDbService.beginTx();
        try {
            taxonNode = new TaxonNode(graphDbService.createNode(), taxon.getName());
            taxonNode.setExternalId(taxon.getExternalId());
            taxonNode.setPath(taxon.getPath());
            taxonNode.setPathNames(taxon.getPathNames());
            taxonNode.setCommonNames(taxon.getCommonNames());
            taxonNode.setRank(taxon.getRank());
            addToIndeces(taxonNode, taxon.getName());
            transaction.success();
        } finally {
            transaction.finish();
        }
        return taxonNode;
    }

    public TaxonNode createTaxonNoTransaction(String name, String externalId, String path) {
        Node node = graphDbService.createNode();
        TaxonNode taxon = new TaxonNode(node, corrector.correct(name));
        if (null != externalId) {
            taxon.setExternalId(externalId);
        }
        if (null != path) {
            taxon.setPath(path);
        }
        addToIndeces(taxon, taxon.getName());
        return taxon;
    }

    public void setEnricher(PropertyEnricher enricher) {
        this.enricher = enricher;
    }

    public void setCorrector(CorrectionService corrector) {
        this.corrector = corrector;
    }

    public IndexHits<Node> findCloseMatchesForTaxonName(String taxonName) {
        return NodeUtil.query(taxonName, PropertyAndValueDictionary.NAME, taxons);
    }

    public IndexHits<Node> suggestTaxaByName(String wholeOrPartialScientificOrCommonName) {
        return taxonNameSuggestions.query("name:\"" + wholeOrPartialScientificOrCommonName + "\"");
    }

    private void addToIndeces(TaxonNode taxon, String indexedName) {
        if (StringUtils.isNotBlank(indexedName)) {
            if (!StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, indexedName)
                    && !StringUtils.equals(PropertyAndValueDictionary.NO_NAME, indexedName)) {
                taxons.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, indexedName);
            }

            String externalId = taxon.getExternalId();
            if (!StringUtils.equals(PropertyAndValueDictionary.NO_MATCH, externalId)) {
                taxons.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.EXTERNAL_ID, externalId);
            }
            indexCommonNames(taxon);
            indexTaxonPath(taxon);
        }
    }

    private void indexTaxonPath(TaxonNode taxon) {
        String path = taxon.getPath();
        if (StringUtils.isNotBlank(path)) {
            String[] pathElementArray = path.split(CharsetConstant.SEPARATOR);
            for (String pathElement : pathElementArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, StringUtils.lowerCase(pathElement));
            }
        }
    }

    private void indexCommonNames(TaxonNode taxon) {
        String commonNames = taxon.getCommonNames();
        if (StringUtils.isNotBlank(commonNames)) {
            String[] commonNameArray = commonNames.split(CharsetConstant.SEPARATOR);
            for (String commonName : commonNameArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, StringUtils.lowerCase(commonName));
            }
        }
    }
}
