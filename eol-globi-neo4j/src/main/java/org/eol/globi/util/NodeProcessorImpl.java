package org.eol.globi.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NodeProcessorImpl implements NodeProcessor<NodeListener> {

    private final GraphDatabaseService graphService;
    private final Long batchSize;
    private final String queryKey;
    private final String queryOrQueryObject;
    private final String indexName;

    public NodeProcessorImpl(GraphDatabaseService graphService, Long batchSize, String queryKey, String queryOrQueryObject) {
        this(graphService, batchSize, queryKey, queryOrQueryObject, "studies");
    }

    public NodeProcessorImpl(GraphDatabaseService graphService,
                             Long batchSize,
                             String queryKey,
                             String queryOrQueryObject,
                             String indexName) {
        this.graphService = graphService;
        this.batchSize = batchSize;
        this.queryKey = queryKey;
        this.queryOrQueryObject = queryOrQueryObject;
        this.indexName = indexName;
    }

    @Override
    public void process(NodeListener listener) {
        List<Long> nodeIds;
        final AtomicLong nodeCount = new AtomicLong(0L);
        while (!(nodeIds = NodeUtil.getBatchOfNodes(graphService,
                nodeCount.get(),
                batchSize,
                queryKey,
                queryOrQueryObject,
                indexName)).isEmpty()) {
            for (Long nodeId : nodeIds) {
                try (Transaction tx = graphService.beginTx()) {
                    listener.on(graphService.getNodeById(nodeId));
                    tx.success();
                }
                nodeCount.incrementAndGet();
            }
        }

    }
}
