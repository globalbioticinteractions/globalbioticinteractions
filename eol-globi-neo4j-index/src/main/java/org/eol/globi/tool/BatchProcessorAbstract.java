package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.eol.globi.db.GraphServiceFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BatchProcessorAbstract implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessorAbstract.class);
    protected final GraphServiceFactory factory;
    private Long batchSize = 1000L;

    public BatchProcessorAbstract(GraphServiceFactory factory) {
        this.factory = factory;
    }

    protected abstract String getTotalToBeProcessedQuery();

    protected abstract String getNextBatchQuery(Long batchSize);

    public static String getProgressMsg(Long count, long duration) {
        return String.format("in [%.2f] s at [%.2f] taxon/s ",
                duration / 1000.0,
                (float) count * 1000.0 / duration
        );
    }

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    public void resolveNames(Long batchSize, GraphDatabaseService graphService) {
        long totalToBeProcessed = 0;
        try (Transaction tx = graphService.beginTx()) {
            Result result = tx.execute(getTotalToBeProcessedQuery());
            if (result.hasNext()) {
                totalToBeProcessed = Long.parseLong(result.next().get("totalToBeProcessed").toString());
            }
            tx.commit();
        }

        if (totalToBeProcessed == 0) {
            LOG.info("no unprocessed verbatim taxon names: nothing to do.");
        } else {
            StopWatch watchForEntireRun = new StopWatch();
            watchForEntireRun.start();
            StopWatch watchForBatch = new StopWatch();
            watchForBatch.start();
            final AtomicLong nameCount = new AtomicLong(0L);
            while (processNextBatch(batchSize, nameCount, graphService)) {
                // ignore
                if (totalToBeProcessed < nameCount.get()) {
                    LOG.info("stop name processing: processed more names (i.e., {}) than expected (i.e., {}).", nameCount.get(), totalToBeProcessed);
                    break;
                }
                watchForBatch.stop();
                final long duration = watchForBatch.getTime();
                if (duration > 0) {
                    LOG.info("resolved batch of [{}] names {} ({} names resolved so far or {}%)",
                            batchSize,
                            BatchProcessorAbstract.getProgressMsg(batchSize, duration),
                            nameCount.get(),
                            String.format("%.1f", 100 * nameCount.get() / (float) totalToBeProcessed));
                }
                watchForBatch.reset();
                watchForBatch.start();
            }
            watchForEntireRun.stop();
            LOG.info("resolved [{}] names {}",
                    nameCount,
                    BatchProcessorAbstract.getProgressMsg(nameCount.get(), watchForEntireRun.getTime()));
        }

    }

    private boolean processNextBatch(Long batchSize,
                                     AtomicLong nameCount,
                                     GraphDatabaseService graphService) {
        long numberOfNamesProcessed = 0;
        try (Transaction tx = graphService.beginTx()) {
            Result execute = tx.execute(getNextBatchQuery(batchSize));
            while (execute.hasNext()) {
                numberOfNamesProcessed += 1;
                Map<String, Object> next = execute.next();
                boolean handled = handleResultRow(next, tx);
                if (handled) {
                    nameCount.incrementAndGet();
                }
            }
            tx.commit();
        }
        return numberOfNamesProcessed == batchSize;
    }

    protected abstract boolean handleResultRow(Map<String, Object> next, Transaction tx);

    @Override
    public void index() {
        LOG.info("name resolving started...");
        resolveNames(batchSize, factory.getGraphService());
        LOG.info("name resolving complete.");

    }
}
