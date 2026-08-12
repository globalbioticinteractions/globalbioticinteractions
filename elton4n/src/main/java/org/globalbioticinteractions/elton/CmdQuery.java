package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdNeo4J;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import picocli.CommandLine;

@CommandLine.Command(
        name = "query",
        aliases = {"cypher", "q"},
        description = "query local elton graph"
)
public class CmdQuery extends CmdNeo4J {

    @CommandLine.Option(
            names = {"-c"},
            defaultValue = "MATCH(n) RETURN n limit 10;",
            description = "query species interaction graph"
    )
    String statement;

    @Override
    public void run() {
        GraphDatabaseService graphService = getGraphServiceFactory().getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            Result query1 = tx.execute(statement);
            System.out.println(query1.resultAsString());
        }
    }
    
}
