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

    private final NodeFactoryFactory nodeFactoryFactory;

    private final GraphServiceFactory graphServiceFactory;

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_GRAPHDB_DIR},
            description = "location of neo4j graph.db"
    )
    private String graphDbDir = "./graph.db";

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_NEO4J_VERSION},
            description = "version neo4j index to use",
            hidden = true
    )
    private String neo4jVersion = "2";


    public CmdNeo4J() {
        this.graphServiceFactory =
                getGraphServiceFactory(graphDbDir);
        this.nodeFactoryFactory = getNodeFactoryFactory(neo4jVersion, graphServiceFactory);
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
        return nodeFactoryFactory;
    }

    protected GraphServiceFactory getGraphServiceFactory() {
        return graphServiceFactory;
    }

}
