package org.globalbioticinteractions.elton;

import org.apache.commons.io.IOUtils;
import org.eol.globi.tool.CmdNeo4J;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(
        name = "query",
        aliases = {"cypher", "q"},
        description = "query local elton graph: echo \"MATCH(n) RETURN n LIMIT 1;\""
)
public class CmdQuery extends CmdNeo4J {

    private InputStream stdin;

    @Override
    public void run() {
        GraphDatabaseService graphService = getGraphServiceFactory().getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            try (Result query = tx.execute(IOUtils.toString(getStdin(), StandardCharsets.UTF_8))) {
                System.out.println(query.resultAsString());
                tx.commit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private InputStream getStdin() {
        return stdin == null ? System.in : stdin;
    }

    protected void setStdin(InputStream stdin) {
        this.stdin = stdin;
    }

}
