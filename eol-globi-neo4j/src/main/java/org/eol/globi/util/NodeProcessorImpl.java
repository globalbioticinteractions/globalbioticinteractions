package org.eol.globi.util;

import org.apache.commons.lang3.time.StopWatch;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LOG.info("collecting [" + indexName + "] node ids...");

        DB db = null;

        try {
            db = DBMaker
                    .newMemoryDirectDB()
                    .make();
            DB.BTreeSetMaker treeSet =
                    db.createTreeSet(UUID.randomUUID().toString());

            NavigableSet<Long> ids = treeSet.makeLongSet();
            NodeUtil.collectIds(graphService, queryKey, queryOrQueryObject, indexName, ids);

            logBatchFinishStats(stopWatch, ids.size(), "collected", this.indexName);

            batchListener.onStart();

            LOG.info("processing " + ids.size() + " [" + indexName + "] nodes...");

            for (Long nodeId : ids) {
                nodeListener.on(graphService.getNodeById(nodeId));
                nodeCount.incrementAndGet();
                if (nodeCount.get() % batchSize == 0) {
                    batchListener.onStart();
                }
            }

            batchListener.onFinish();
            logBatchFinishStats(stopWatch, nodeCount.get(), "processed", this.indexName);
            stopWatch.stop();
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public void logBatchFinishStats(StopWatch stopWatch, long count, String verb, String indexName) {
        LOG.info(verb + " " + count + " " + "[" + indexName + "] nodes in " + stopWatch.getTime()/1000 + "s (@ " + count / (stopWatch.getTime()+1) + " nodes/ms)");
    }
}
