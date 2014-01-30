package org.eol.globi.data.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryParser.QueryParser;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonMatchValidator;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

public class TaxonServiceImpl implements TaxonService {
    private final GraphDatabaseService graphDbService;
    private final Index<Node> taxons;
    private final Index<Node> taxonNameSuggestions;
    private final Index<Node> taxonPaths;
    private final Index<Node> taxonCommonNames;
    private CorrectionService corrector;
    private TaxonPropertyEnricher enricher;

    public TaxonServiceImpl(TaxonPropertyEnricher taxonEnricher, CorrectionService correctionService, GraphDatabaseService graphDbService) {
        this.enricher = taxonEnricher;
        this.corrector = correctionService;
        this.graphDbService = graphDbService;
        this.taxons = graphDbService.index().forNodes("taxons");
        this.taxonNameSuggestions = graphDbService.index().forNodes("taxonNameSuggestions");
        this.taxonPaths = graphDbService.index().forNodes("taxonpaths", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.taxonCommonNames = graphDbService.index().forNodes("taxonCommonNames", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
    }

    @Override
    public TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        if (StringUtils.length(name) < 2) {
            throw new NodeFactoryException("taxon name [" + name + "] must contains more than 1 character");
        }
        TaxonNode taxon = findTaxon(name);
        return taxon == null ? createTaxon(name, externalId, path) : taxon;
    }

    @Override
    public TaxonNode findTaxon(String taxonName) throws NodeFactoryException {
        String cleanedTaxonName = corrector.correct(taxonName);
        String query = "name:\"" + QueryParser.escape(cleanedTaxonName) + "\"";
        IndexHits<Node> matchingTaxa = taxons.query(query);
        Node matchingTaxon;
        TaxonNode firstMatchingTaxon = null;
        if (matchingTaxa.hasNext()) {
            matchingTaxon = matchingTaxa.next();
            firstMatchingTaxon = new TaxonNode(matchingTaxon);
        }
        if (matchingTaxa.hasNext()) {
            throw new NodeFactoryException("found duplicate taxon for [" + taxonName + "] (original name: [" + taxonName + "]");
        }
        matchingTaxa.close();


        return firstMatchingTaxon;
    }

    private TaxonNode createTaxon(String name, String externalId, String path) throws NodeFactoryException {
        String correctedName = corrector.correct(name);

        Taxon taxon = new TaxonImpl();
        taxon.setName(correctedName);
        taxon.setExternalId(externalId);
        taxon.setPath(path);

        TaxonNode taxonNode = null;
        while (taxonNode == null) {
            enricher.enrich(taxon);
            taxonNode = findTaxon(taxon.getName());
            if (taxonNode == null) {
                if (TaxonMatchValidator.hasMatch(taxon)) {
                    taxonNode = addTaxon(taxon);
                } else {
                    String truncatedName = NodeUtil.truncateTaxonName(taxon.getName());
                    if (truncatedName == null || StringUtils.length(truncatedName) < 3) {
                        taxonNode = addNoMatchTaxon(externalId, path, correctedName);
                    } else {
                        taxon = new TaxonImpl();
                        taxon.setName(truncatedName);
                    }
                }
            } else {
                if (!StringUtils.equals(correctedName, taxonNode.getName())) {
                    addAltenateNameToIndex(taxonNode, correctedName);
                }
            }
        }
        return taxonNode;
    }

    private void addAltenateNameToIndex(TaxonNode taxonNode, String alternateName) {
        Transaction tx = taxonNode.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            if (StringUtils.isNotBlank(alternateName)) {
                taxons.add(taxonNode.getUnderlyingNode(), PropertyAndValueDictionary.NAME, alternateName);
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private TaxonNode addNoMatchTaxon(String externalId, String path, String correctedName) {
        Taxon noMatchTaxon = new TaxonImpl();
        noMatchTaxon.setName(correctedName);
        noMatchTaxon.setExternalId(StringUtils.isBlank(externalId) ? PropertyAndValueDictionary.NO_MATCH : externalId);
        noMatchTaxon.setPath(path);
        return addTaxon(noMatchTaxon);
    }

    private TaxonNode addTaxon(Taxon taxon) {
        TaxonNode taxonNode = null;
        Transaction transaction = graphDbService.beginTx();
        try {
            taxonNode = new TaxonNode(graphDbService.createNode(), taxon.getName());
            taxonNode.setExternalId(taxon.getExternalId());
            taxonNode.setPath(taxon.getPath());
            taxonNode.setCommonNames(taxon.getCommonNames());
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

    public void setEnricher(TaxonPropertyEnricher enricher) {
        this.enricher = enricher;
    }

    public void setCorrector(CorrectionService corrector) {
        this.corrector = corrector;
    }

    public IndexHits<Node> findCloseMatchesForTaxonName(String taxonName) {
        return NodeUtil.query(taxonName, PropertyAndValueDictionary.NAME, taxons);
    }

    public IndexHits<Node> findCloseMatchesForTaxonPath(String taxonPath) {
        return NodeUtil.query(taxonPath, PropertyAndValueDictionary.PATH, taxonPaths);
    }

    public IndexHits<Node> findTaxaByPath(String wholeOrPartialPath) {
        return taxonPaths.query("path:\"" + wholeOrPartialPath + "\"");
    }

    public IndexHits<Node> findTaxaByCommonName(String wholeOrPartialName) {
        return taxonCommonNames.query("commonNames:\"" + wholeOrPartialName + "\"");
    }

    public IndexHits<Node> suggestTaxaByName(String wholeOrPartialScientificOrCommonName) {
        return taxonNameSuggestions.query("name:\"" + wholeOrPartialScientificOrCommonName + "\"");
    }

    private void addToIndeces(TaxonNode taxon, String indexedName) {
        if (StringUtils.isNotBlank(indexedName)) {
            taxons.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, indexedName);
            indexCommonNames(taxon);
            indexTaxonPath(taxon);
        }
    }

    private void indexTaxonPath(TaxonNode taxon) {
        String path = taxon.getPath();
        if (StringUtils.isNotBlank(path)) {
            taxonPaths.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.PATH, path);
            taxonCommonNames.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.PATH, path);
            String[] pathElementArray = path.split(CharsetConstant.SEPARATOR);
            for (String pathElement : pathElementArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, StringUtils.lowerCase(pathElement));
            }
        }
    }

    private void indexCommonNames(TaxonNode taxon) {
        String commonNames = taxon.getCommonNames();
        if (StringUtils.isNotBlank(commonNames)) {
            taxonCommonNames.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.COMMON_NAMES, commonNames);
            String[] commonNameArray = commonNames.split(CharsetConstant.SEPARATOR);
            for (String commonName : commonNameArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), PropertyAndValueDictionary.NAME, StringUtils.lowerCase(commonName));
            }
        }
    }
}
