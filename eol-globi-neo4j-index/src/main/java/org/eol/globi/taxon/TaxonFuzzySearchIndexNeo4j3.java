package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.NoSuchElementException;

public class TaxonFuzzySearchIndexNeo4j3 implements TaxonFuzzySearchIndex {
    public static final String TAXON_NAME_SUGGESTIONS = "taxonNameSuggestions";
    private final GraphDatabaseService graphDbService;

    public TaxonFuzzySearchIndexNeo4j3(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
        try (Transaction tx = graphDbService.beginTx()) {
            Result execute = tx.execute("CALL db.indexes YIELD indexName");
            ResourceIterator<String> indexName = execute.columnAs("indexName");
            long size = indexName
                    .stream()
                    .filter(name -> StringUtils.equals(TAXON_NAME_SUGGESTIONS, name))
                    .limit(1)
                    .count();
            if (size == 0) {

                tx.execute("CALL db.index.fulltext.createNodeIndex(" +
                        "'" + TAXON_NAME_SUGGESTIONS + "', " +
                        "['" + NodeLabel.Taxon.name() + "'], " +
                        "['" + PropertyAndValueDictionary.COMMON_NAMES + "','" + PropertyAndValueDictionary.NAME + "'])");
            }

            tx.commit();
        }
    }

    @Override
    public ResourceIterator<Node> query(String luceneQueryString) {
        try(Transaction transaction = graphDbService.beginTx()) {
            Result execute = transaction.execute("CALL db.index.fulltext.queryNodes(" +
                    "\"" + TAXON_NAME_SUGGESTIONS + "\"" +
                    ", \"" + StringUtils.replace(luceneQueryString, "name:", "") + "\")");

            return execute.hasNext()
                    ? execute.columnAs("node")
                    : new ResourceIterator<Node>() {
                @Override
                public void close() {

                }

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Node next() {
                    throw new NoSuchElementException("empty resource");
                }
            };

        }
    }

    @Override
    public void indexTaxonByNames(Node indexNode, String names) {
        //
    }

    @Override
    public void index(Node indexNode, TaxonNode taxonNode) {
        indexTaxonByNames(indexNode, taxonNode.getCommonNames());
        indexTaxonByNames(indexNode, taxonNode.getPath());
    }

}
