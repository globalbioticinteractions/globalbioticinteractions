package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.TaxonFuzzySearchIndex;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LinkerTaxonIndex implements IndexerNeo4j {

    public static final String INDEX_TAXON_NAMES_AND_IDS = "taxonPaths";


    @Override
    public void index(GraphServiceFactory factory) {
        GraphDatabaseService graphDb = factory.getGraphService();
        Transaction tx = graphDb.beginTx();
        try {
            Index<Node> taxons = graphDb.index().forNodes("taxons");
            Index<Node> ids = graphDb.index().forNodes(INDEX_TAXON_NAMES_AND_IDS, MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
            TaxonFuzzySearchIndex fuzzySearchIndex = new TaxonFuzzySearchIndex(graphDb);
            IndexHits<Node> hits = taxons.query("*:*");
            for (Node hit : hits) {
                List<String> taxonIds = new ArrayList<>();
                List<String> taxonPathIdsAndNames = new ArrayList<>();
                TaxonNode taxonNode = new TaxonNode(hit);
                addTaxonId(taxonIds, taxonNode);
                addPathIdAndNames(taxonPathIdsAndNames, taxonNode);

                addToFuzzyIndex(graphDb, fuzzySearchIndex, hit, taxonNode);

                Iterable<Relationship> rels = hit.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.SAME_AS));
                for (Relationship rel : rels) {
                    TaxonNode sameAsTaxon = new TaxonNode(rel.getEndNode());
                    addTaxonId(taxonIds, sameAsTaxon);
                    addPathIdAndNames(taxonPathIdsAndNames, sameAsTaxon);
                    addToFuzzyIndex(graphDb, fuzzySearchIndex, hit, sameAsTaxon);
                }
                taxonPathIdsAndNames.addAll(taxonIds);
                String aggregateIds = StringUtils.join(taxonPathIdsAndNames.stream().distinct().sorted().collect(Collectors.toList()), CharsetConstant.SEPARATOR);
                ids.add(hit, PropertyAndValueDictionary.PATH, aggregateIds);
                hit.setProperty(PropertyAndValueDictionary.EXTERNAL_IDS, aggregateIds);

                String aggregateTaxonIds = StringUtils.join(taxonIds.stream().distinct().sorted().collect(Collectors.toList()), CharsetConstant.SEPARATOR);
                hit.setProperty(PropertyAndValueDictionary.NAME_IDS, aggregateTaxonIds);
                tx.success();
                tx.close();
                tx = graphDb.beginTx();
            }
            hits.close();
            tx.success();
        } finally {
            tx.close();
        }
    }

    private void addToFuzzyIndex(GraphDatabaseService graphDb, TaxonFuzzySearchIndex fuzzySearchIndex, Node indexNode, TaxonNode taxonNode) {
        try (Transaction tx = graphDb.beginTx()) {
            fuzzySearchIndex.index(indexNode, taxonNode);
            tx.success();
        }
    }

    private void addTaxonId(List<String> externalIds, TaxonNode taxonNode) {
        String externalId = taxonNode.getExternalId();
        if (StringUtils.isNotBlank(externalId)) {
            externalIds.add(externalId);
        }
    }

    private void addPathIdAndNames(List<String> externalIds, TaxonNode taxonNode) {
        if (StringUtils.isNotBlank(taxonNode.getName())) {
            externalIds.add(taxonNode.getName());
        }
        addDelimitedList(externalIds, taxonNode.getPath());
        addDelimitedList(externalIds, taxonNode.getPathIds());
    }

    private void addDelimitedList(List<String> externalIds, String path) {
        String[] pathElements = StringUtils.splitByWholeSeparator(path, CharsetConstant.SEPARATOR);
        if (pathElements != null) {
            externalIds.addAll(Arrays.asList(pathElements));
        }
    }
}
