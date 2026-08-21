package org.globalbioticinteractions.elton;

import org.eol.globi.tool.CmdNeo4J;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class CmdCreateLocationIndexes extends CmdNeo4J {

    @Override
    public void run() {
        GraphDatabaseService graphService = getGraphServiceFactory().getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            Neo4jIndexUtil.createPointLocationIndexIfNotExists(tx);
            tx.commit();
        }
    }
}
