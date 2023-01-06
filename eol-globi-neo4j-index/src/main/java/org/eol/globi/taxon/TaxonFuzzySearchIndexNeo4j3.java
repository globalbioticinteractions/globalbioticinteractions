package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.cypher.internal.compiler.v3_1.EmptyResourceIterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class TaxonFuzzySearchIndexNeo4j3 implements TaxonFuzzySearchIndex {
    public static final String TAXON_NAME_SUGGESTIONS = "taxonNameSuggestions";
    private final GraphDatabaseService graphDbService;

    public TaxonFuzzySearchIndexNeo4j3(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
        try (Transaction tx = graphDbService.beginTx()) {
            Result execute = graphDbService.execute("CALL db.indexes YIELD indexName");
            ResourceIterator<String> indexName = execute.columnAs("indexName");
            long size = indexName
                    .stream()
                    .filter(name -> StringUtils.equals(TAXON_NAME_SUGGESTIONS, name))
                    .limit(1)
                    .count();
            if (size == 0) {

                graphDbService.execute("CALL db.index.fulltext.createNodeIndex(" +
                        "'" + TAXON_NAME_SUGGESTIONS + "', " +
                        "['" + NodeLabel.Taxon.name() + "'], " +
                        "['" + PropertyAndValueDictionary.COMMON_NAMES + "','" + PropertyAndValueDictionary.NAME + "'])");
            }

            tx.success();
        }
    }

    @Override
    public ResourceIterator<Node> query(String luceneQueryString) {
        ;
        Result execute = graphDbService.execute("CALL db.index.fulltext.queryNodes(" +
                "\"" + TAXON_NAME_SUGGESTIONS + "\"" +
                ", \"" + StringUtils.replace(luceneQueryString, "name:", "") + "\")");

        return execute.hasNext()
                ? execute.columnAs("node")
                : new EmptyResourceIterator<>();
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
