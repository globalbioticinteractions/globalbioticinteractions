package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GraphServiceFactoryImpl implements GraphServiceFactory {

    private final static Logger LOG = LoggerFactory.getLogger(GraphServiceFactoryImpl.class);

    private final File graphDbDir;
    private static GraphDatabaseService graphDb;

    public GraphServiceFactoryImpl(File graphDbDir) {
        if (graphDb != null) {
            throw new IllegalStateException("only one graph service factorySkipBOM allowed, but another is already instantiated");
        }
        this.graphDbDir = graphDbDir;
    }

    @Override
    public GraphDatabaseService getGraphService() {
        GraphServiceUtil.verifyState(graphDb);

        if (graphDb == null) {
            graphDb = startNeo4j(graphDbDir);
        }
        return graphDb;
    }

    @Override
    public void close() {
        if (graphDb != null) {
            graphDb.shutdown();
            graphDb = null;
        }
    }

    private static GraphDatabaseService startNeo4j(File graphDbDir) {
        LOG.info("neo4j starting at [" + graphDbDir.getAbsolutePath() + "]...");

        final GraphDatabaseService graphService = new GraphDatabaseFactory()
                .setUserLogProvider(new Slf4jLogProvider())
                .newEmbeddedDatabaseBuilder(graphDbDir)
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "keep_none")
                .setConfig(GraphDatabaseSettings.logical_log_rotation_threshold, "250M")
                // note that according to https://neo4j.com/developer/kb/checkpointing-and-log-pruning-interactions/#_triggering_of_checkpointing_and_pruning_events
                // volumetric checkpointing is not supported in the community edition
                //.setConfig(GraphDatabaseSettings.check_point_policy, "volumetric")
                .setConfig(GraphDatabaseSettings.check_point_interval_time, "60s")
//                .setConfig(GraphDatabaseSettings.check_point_interval_tx, "100000")
                // peg pagecache size to the provided jvm max memory
                // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/995
                .setConfig(GraphDatabaseSettings.pagecache_memory, Long.toString(Runtime.getRuntime().maxMemory()))
                .newGraphDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("neo4j stopping...");
            if (graphService.isAvailable(0)) {
                graphService.shutdown();
            }
            LOG.info("done.");
        }));
        LOG.info("done");
        return graphService;
    }

}