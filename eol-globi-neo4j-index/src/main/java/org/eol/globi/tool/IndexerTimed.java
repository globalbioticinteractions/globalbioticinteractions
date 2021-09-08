package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.eol.globi.data.StudyImporterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.db.GraphServiceFactory;
import org.neo4j.graphdb.GraphDatabaseService;

public class IndexerTimed implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerTimed.class);
    private final IndexerNeo4j indexer;

    public IndexerTimed(IndexerNeo4j indexer) {
        this.indexer = indexer;
    }

    @Override
    public void index() throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String linkName = indexer.getClass().getSimpleName();
        LOG.info(linkName + " started...");
        try {
            indexer.index();
        } finally {
            stopWatch.stop();
            LOG.info(linkName + " completed in [" + stopWatch.getTime() / 1000 + "]s");
        }
    }
}
