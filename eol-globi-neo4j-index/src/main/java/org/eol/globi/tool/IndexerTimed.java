package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.db.GraphServiceFactory;
import org.neo4j.graphdb.GraphDatabaseService;

public class IndexerTimed implements IndexerNeo4j {
    private static final Log LOG = LogFactory.getLog(IndexerTimed.class);
    private final IndexerNeo4j indexer;

    public IndexerTimed(IndexerNeo4j indexer) {
        this.indexer = indexer;
    }

    @Override
    public void index(GraphServiceFactory graphService) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String linkName = indexer.getClass().getSimpleName();
        LOG.info(linkName + " started...");
        try {
            indexer.index(graphService);
        } finally {
            stopWatch.stop();
            LOG.info(linkName + " completed in [" + stopWatch.getTime() / 1000 + "]s");
        }
    }
}
