package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryImpl;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.DatasetRegistry;
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
            names = {CmdOptionConstants.OPTION_GRAPHDB_DIR},
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


    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_NEO4J_VERSION},
            description = "version neo4j index to use (NOTE: only v2 indexes are fully implemented currently, v2 indexes work with neo4j v3.5.x)",
            defaultValue = "2",
            hidden = true
    )
    private String neo4jVersion;

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_TAXON_CACHE_PATH},
            defaultValue = "./taxonCache.tsv.gz",
            description = "location of taxonCache.tsv.gz"
    )
    private String taxonCachePath;

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_TAXON_MAP_PATH},
            defaultValue = "./taxonMap.tsv.gz",
            description = "location of taxonMap.tsv.gz"
    )
    private String taxonMapPath;

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
        if (this.graphServiceFactory == null) {
            this.graphServiceFactory =
                    getGraphServiceFactory(graphDbDir);
        }
        return graphServiceFactory;
    }

    public void setNodeFactoryFactory(NodeFactoryFactory nodeFactoryFactory) {
        this.nodeFactoryFactory = nodeFactoryFactory;
    }

    public void setGraphServiceFactory(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    protected void configureAndRun(CmdNeo4J cmd) {
        cmd.setTaxonCachePath(getTaxonCachePath());
        cmd.setTaxonMapPath(getTaxonMapPath());
        cmd.setGraphServiceFactory(getGraphServiceFactory());
        cmd.setNodeFactoryFactory(getNodeFactoryFactory());
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
}
