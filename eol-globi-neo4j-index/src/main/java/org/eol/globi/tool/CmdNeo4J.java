package org.eol.globi.tool;

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
            defaultValue = ".neo4j/graph.db",
            description = "location of neo4j graph.db"
    )
    private String graphDbDir;

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_DATASET_DIR},
            defaultValue = "datasets",
            description = "location of Elton tracked data content"
    )
    private String datasetDir;

    @CommandLine.Option(
            names = {"-provDir"},
            defaultValue = "datasets",
            description = "location of Elton tracked data provenance"
    )
    private String provenanceDir;

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
            names = {"-exportDir"},
            defaultValue = "export",
            description = "directory to save elton4n data export products into"
    )
    private String baseDir;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }


    private static NodeFactoryFactory getNodeFactoryFactory(GraphServiceFactory graphServiceFactory) {
        return new NodeFactoryFactoryTransactingOnDataset(graphServiceFactory);
    }

    private static GraphServiceFactoryImpl getGraphServiceFactory(String graphDbDir) {
        return new GraphServiceFactoryImpl(
                new File(graphDbDir));
    }

    protected NodeFactoryFactory getNodeFactoryFactory() {
        if (this.nodeFactoryFactory == null) {
            this.nodeFactoryFactory = getNodeFactoryFactory(getGraphServiceFactory());
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

    @Override
    public void destroy() {
        try {
            if (CmdNeo4J.graphServiceFactory != null) {
                CmdNeo4J.graphServiceFactory.close();
                CmdNeo4J.graphServiceFactory = null;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    protected void configureRunAndDestroy(CmdNeo4J cmd) {
        try {
            configureAndRun(cmd);
        } finally {
            //cmd.destroy();
        }
    }

    protected void configureAndRun(CmdNeo4J cmd) {
        cmd.setTaxonCachePath(getTaxonCachePath());
        cmd.setTaxonMapPath(getTaxonMapPath());
        cmd.setGraphServiceFactory(getGraphServiceFactory());
        cmd.setNodeFactoryFactory(getNodeFactoryFactory());
        cmd.setCacheDir(getCacheDir());
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

    public String getProvenanceDir() {
        return provenanceDir;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

}
