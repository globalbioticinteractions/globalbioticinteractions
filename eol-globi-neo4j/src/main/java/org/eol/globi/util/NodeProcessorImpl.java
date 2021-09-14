package org.eol.globi.util;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NodeProcessorImpl implements NodeProcessor<NodeListener> {

    private final static Logger LOG = LoggerFactory.getLogger(NodeProcessorImpl.class);

    private final GraphDatabaseService graphService;
    private final Long batchSize;
    private final String queryKey;
    private final String queryOrQueryObject;
    private final String indexName;

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
        process(listener, createBatchListenerNoop());
    }

    private BatchListener createBatchListenerNoop() {
        return new BatchListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFinish() {

            }
        };
    }

    public void process(NodeListener nodeListener, BatchListener batchListener) {
        List<Long> nodeIds;
        final AtomicLong nodeCount = new AtomicLong(0L);
        do {
            batchListener.onStart();
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            LOG.info("requesting batch of [" + batchSize + "] [" + indexName + "] nodes");
            nodeIds = NodeUtil.getBatchOfNodes(graphService,
                    nodeCount.get(),
                    batchSize,
                    queryKey,
                    queryOrQueryObject,
                    indexName);
            stopWatch.split();
            LOG.info("received batch of [" + batchSize + "] [" + indexName + "] nodes in [" + stopWatch.getTime() +"] ms");
            for (Long nodeId : nodeIds) {
                nodeListener.on(graphService.getNodeById(nodeId));
                nodeCount.incrementAndGet();
            }
            batchListener.onFinish();
            stopWatch.stop();
            LOG.info("processed batch of [" + batchSize + "] [" + indexName + "] nodes in [" + stopWatch.getTime() +"] ms.");
            LOG.info("total processed [" + indexName + "] nodes so far:  [" + nodeCount.get() +"]");
        } while (!nodeIds.isEmpty());
    }
}
