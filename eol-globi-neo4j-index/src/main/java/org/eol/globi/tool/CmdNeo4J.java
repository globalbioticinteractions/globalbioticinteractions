package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryImpl;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "compile",
        aliases = {"import"},
        description = "compile and import datasets into Neo4J"
)
public abstract class CmdNeo4J implements Cmd {


    private NodeFactoryFactory nodeFactoryFactory = null;

    private static GraphServiceFactory graphServiceFactory = null;

    @CommandLine.Option(
            names = {"-graphDbDir"},
            defaultValue = "./graph.db",
            description = "location of neo4j graph.db"
    )
    private String graphDbDir;

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_DATASET_DIR},
            defaultValue = "./datasets",
            description = "location of Elton tracked datasets"
    )
    private String datasetDir;


    public void setNeo4jVersion(String neo4jVersion) {
        this.neo4jVersion = neo4jVersion;
    }

    @CommandLine.Option(
            names = {"-neo4jVersion"},
            description = "version neo4j index to use (NOTE: only v2 indexes are fully implemented currently, v2 indexes work with neo4j v3.5.x)",
            defaultValue = "2",
            hidden = true
    )
    private String neo4jVersion;

    @CommandLine.Option(
            names = {"-taxonCache"},
            defaultValue = "classpath:/org/eol/globi/tool/taxonCacheEmpty.tsv",
            description = "location of taxonCache.tsv"
    )
    private String taxonCachePath;

    @CommandLine.Option(
            names = {"-taxonMap"},
            defaultValue = "classpath:/org/eol/globi/tool/taxonMapEmpty.tsv",
            description = "location of taxonMap.tsv"
    )
    private String taxonMapPath;

    @CommandLine.Option(
            names = {"-nameIndexCache"},
            defaultValue = "./taxonIndexCache",
            description = "location of cached taxon index"
    )
    private String cacheDir;


    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_EXPORT_DIR},
            defaultValue = ".",
            description = "location of neo4j graph.db"
    )
    private String baseDir;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }


    private static NodeFactoryFactory getNodeFactoryFactory(String neo4jVersion, GraphServiceFactory graphServiceFactory) {
        return StringUtils.equals("2", neo4jVersion)
                ? new NodeFactoryFactoryTransactingOnDatasetNeo4j2(graphServiceFactory)
                : new NodeFactoryFactoryTransactingOnDatasetNeo4j3(graphServiceFactory);
    }

    private static GraphServiceFactoryImpl getGraphServiceFactory(String graphDbDir) {
        return new GraphServiceFactoryImpl(
                new File(graphDbDir));
    }

    protected NodeFactoryFactory getNodeFactoryFactory() {
        if (this.nodeFactoryFactory == null) {
            this.nodeFactoryFactory = getNodeFactoryFactory(neo4jVersion, getGraphServiceFactory());
        }
        return nodeFactoryFactory;
    }

    protected GraphServiceFactory getGraphServiceFactory() {
        if (graphServiceFactory == null) {
            graphServiceFactory =
                    getGraphServiceFactory(graphDbDir);
        }
        return graphServiceFactory;
    }

    public void setNodeFactoryFactory(NodeFactoryFactory nodeFactoryFactory) {
        this.nodeFactoryFactory = nodeFactoryFactory;
    }

    public void setGraphServiceFactory(GraphServiceFactory graphServiceFactory) {
        CmdNeo4J.graphServiceFactory = graphServiceFactory;
    }

    protected void configureAndRun(CmdNeo4J cmd) {
        cmd.setTaxonCachePath(getTaxonCachePath());
        cmd.setTaxonMapPath(getTaxonMapPath());
        cmd.setGraphServiceFactory(getGraphServiceFactory());
        cmd.setNodeFactoryFactory(getNodeFactoryFactory());
        cmd.setCacheDir(getCacheDir());
        cmd.setNeo4jVersion("2");
        cmd.run();
    }

    public String getTaxonCachePath() {
        return taxonCachePath;
    }

    public String getTaxonMapPath() {
        return taxonMapPath;
    }

    public void setTaxonCachePath(String taxonCachePath) {
        this.taxonCachePath = taxonCachePath;
    }

    public void setTaxonMapPath(String taxonMapPath) {
        this.taxonMapPath = taxonMapPath;
    }


    public String getDatasetDir() {
        return datasetDir;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getNeo4jVersion() {
        return neo4jVersion;
    }

}
