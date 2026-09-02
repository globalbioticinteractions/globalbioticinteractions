package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;

public class NodeIdCollectorImpl implements NodeIdCollector {

    private static final Map<String, NodeLabel> INDEX_NAME_TO_LABEL = Collections.unmodifiableMap(new TreeMap<String, NodeLabel>() {{
        put("studies", NodeLabel.Reference);
        put("datasets", NodeLabel.Dataset);
    }});

    @Override
    public void collectIds(GraphDatabaseService graphService,
                           String queryKey,
                           String queryOrQueryObject,
                           String indexName,
                           NavigableSet<Long> ids) {

        if (!INDEX_NAME_TO_LABEL.containsKey(indexName)) {
            throw new IllegalArgumentException("indexName [" + indexName + "] not supported");
        }

        try (Transaction transaction = graphService.beginTx()) {
            ResourceIterator<Node> nodes = transaction.findNodes(
                    INDEX_NAME_TO_LABEL.get(indexName));
            nodes.stream()
                    .filter(n -> n.hasProperty(queryKey))
                    .filter(n -> {
                        System.out.println("found [" + n.getProperty(queryKey) + "]" + " for queryKey in context of [" + queryOrQueryObject + "]");
                        return StringUtils.startsWith(n.getProperty(queryKey).toString(), queryOrQueryObject);
                    })
                    .map(Node::getId)
                    .forEach(ids::add);
            transaction.commit();
        }
    }
}
