package org.eol.globi.util;

import org.eol.globi.data.NodeLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;

public class NodeIdCollectorNeo4j3 implements NodeIdCollector {

    private static final Map<String, NodeLabel> INDEX_NAME_TO_LABEL = Collections.unmodifiableMap(new TreeMap<String, NodeLabel>() {{
        put("taxons", NodeLabel.Taxon);
        put("studies", NodeLabel.Reference);
    }});

    @Override
    public void collectIds(GraphDatabaseService graphService, String queryKey, String queryOrQueryObject, String indexName, NavigableSet<Long> ids) {

        if (!INDEX_NAME_TO_LABEL.containsKey(indexName)) {
            throw new IllegalArgumentException("indexName [" + indexName + "] not supported");
        }

        try (ResourceIterator<Node> nodes = graphService.findNodes(
                INDEX_NAME_TO_LABEL.get(indexName))) {
            nodes.stream()
                    .map(Node::getId)
                    .forEach(ids::add);
        }
    }
}
