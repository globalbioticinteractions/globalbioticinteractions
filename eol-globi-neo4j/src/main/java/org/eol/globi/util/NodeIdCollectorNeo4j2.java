package org.eol.globi.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.NavigableSet;

public class NodeIdCollectorNeo4j2 implements NodeIdCollector {

    @Override
    public void collectIds(GraphDatabaseService graphService, String queryKey, String queryOrQueryObject, String indexName, NavigableSet<Long> ids) {
        Index<Node> index = graphService.index().forNodes(indexName);
        IndexHits<Node> studies = index.query(queryKey, queryOrQueryObject);
        studies
                .stream()
                .map(Node::getId)
                .forEach(ids::add);
        studies.close();
    }
}
