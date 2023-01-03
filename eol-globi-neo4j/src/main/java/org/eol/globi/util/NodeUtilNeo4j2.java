package org.eol.globi.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.NavigableSet;

public class NodeUtilNeo4j2 {

    public static Index<Node> forNodes(GraphDatabaseService graphDb, String indexName) {
        return graphDb.index().forNodes(indexName);
    }

    static void collectIds(GraphDatabaseService graphService, String queryKey, String queryOrQueryObject, String indexName, NavigableSet<Long> ids) {
        Index<Node> index = graphService.index().forNodes(indexName);
        IndexHits<Node> studies = index.query(queryKey, queryOrQueryObject);
        studies
                .stream()
                .map(Node::getId)
                .forEach(ids::add);
        studies.close();
    }
}

