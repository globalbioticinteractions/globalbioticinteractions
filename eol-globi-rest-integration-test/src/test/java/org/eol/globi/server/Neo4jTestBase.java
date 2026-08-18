package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.junit.After;
import org.junit.BeforeClass;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.time.Duration;
import java.util.TreeMap;

public class Neo4jTestBase {

    public static Neo4j neo4j;

    @BeforeClass
    public static void initializeNeo4j() {
        neo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds(30))
                .build();
    }

    @After
    public void deleteAllNodesAndRelations() {
        neo4j.defaultDatabaseService().executeTransactionally("MATCH (n) DETACH DELETE n");
    }

    public void validate(CypherQuery query) {
        TreeMap<String, Object> params = new TreeMap<String, Object>() {
            {
                putAll(query.getParams());
            }

        };
        neo4j.defaultDatabaseService().executeTransactionally(query.getVersionedQuery(), params);
    }

}


