package org.eol.globi.taxon;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexSetting;
import org.neo4j.graphdb.schema.IndexType;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class TaxonFuzzySearchIndexNeo4j implements TaxonFuzzySearchIndex {
    public static final String TAXON_NAME_SUGGESTIONS = "taxonNameSuggestions";
    private final GraphDatabaseService graphDbService;

    public TaxonFuzzySearchIndexNeo4j(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
        try (Transaction tx = graphDbService.beginTx()) {
            tx.schema()
                    .indexFor(NodeLabel.Taxon)
                    .withIndexType(IndexType.FULLTEXT)
                    .withName(TAXON_NAME_SUGGESTIONS)
                    .on(PropertyAndValueDictionary.NAME)
                    .on(PropertyAndValueDictionary.PATH)
                    .on(PropertyAndValueDictionary.EXTERNAL_ID)
                    .create();
            tx.commit();
        }
    }

    @Override
    public ResourceIterator<Node> query(String luceneQueryString) {
        try (Transaction transaction = graphDbService.beginTx()) {

            String query = "CALL db.index.fulltext.queryNodes(" +
                    "\"" + TAXON_NAME_SUGGESTIONS + "\"" +
                    ", \"" + StringUtils.replace(luceneQueryString, "name:", "") + "\")" +
                    " YIELD node, score " +
                    "RETURN node, score";
            Result execute = transaction.execute(query);
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
