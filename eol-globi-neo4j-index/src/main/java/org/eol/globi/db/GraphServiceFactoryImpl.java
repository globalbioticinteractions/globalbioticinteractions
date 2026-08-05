package org.eol.globi.db;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class GraphServiceFactoryImpl implements GraphServiceFactory {

    private final static Logger LOG = LoggerFactory.getLogger(GraphServiceFactoryImpl.class);
    private static DatabaseManagementService databaseManagementService;

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
            graphDb = null;
            databaseManagementService.shutdown();
        }
    }

    private static GraphDatabaseService startNeo4j(File graphDbDir) {
        LOG.info("neo4j starting at [" + graphDbDir.getAbsolutePath() + "]...");

        DatabaseManagementServiceBuilder builder
                = new DatabaseManagementServiceBuilder( graphDbDir.toPath() )
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "keep_none")
                .setConfig(GraphDatabaseSettings.logical_log_rotation_threshold, 250 * 1000000L)
                // note that according to https://neo4j.com/developer/kb/checkpointing-and-log-pruning-interactions/#_triggering_of_checkpointing_and_pruning_events
                // volumetric checkpointing is not supported in the community edition
                //.setConfig(GraphDatabaseSettings.check_point_policy, "volumetric")
                .setConfig(GraphDatabaseSettings.check_point_interval_time, Duration.ofSeconds(60))
//                .setConfig(GraphDatabaseSettings.check_point_interval_tx, "100000")
                // peg pagecache size to the provided jvm max memory
                // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/995
                .setConfig(GraphDatabaseSettings.pagecache_memory, Runtime.getRuntime().maxMemory());

        final DatabaseManagementService databaseManagementService = builder.build();
        final GraphDatabaseService graphDbLocal = databaseManagementService.database(DEFAULT_DATABASE_NAME);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("neo4j stopping...");
            if (graphDbLocal.isAvailable(0)) {
                databaseManagementService.shutdown();
            }
            LOG.info("done.");
        }));
        LOG.info("done");
        return graphDbLocal;
    }

}