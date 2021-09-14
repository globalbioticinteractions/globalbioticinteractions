package org.eol.globi.util;

import org.apache.commons.lang3.time.StopWatch;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;
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
        final AtomicLong nodeCount = new AtomicLong(0L);
        batchListener.onStart();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("collecting [" + indexName + "] node ids");

        DB db = null;

        try {
            db = DBMaker
                    .newMemoryDirectDB()
                    .make();
            DB.BTreeSetMaker treeSet =
                    db.createTreeSet(UUID.randomUUID().toString());

            NavigableSet<Long> ids = treeSet.makeLongSet();
            NodeUtil.collectIds(graphService, queryKey, queryOrQueryObject, indexName, ids);
            LOG.info("received [" + indexName + "] node ids in [" + stopWatch.getTime() + "] ms");
            batchListener.onStart();

            for (Long nodeId : ids) {
                nodeListener.on(graphService.getNodeById(nodeId));
                nodeCount.incrementAndGet();
                if (nodeCount.get() % batchSize == 0) {
                    batchListener.onStart();
                    LOG.info("processed batch of [" + batchSize + "] [" + indexName + "] nodes in [" + stopWatch.getTime() + "] ms.");
                    LOG.info("total processed [" + indexName + "] nodes so far:  [" + nodeCount.get() + "]");
                }
            }

            batchListener.onFinish();
            stopWatch.stop();
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
