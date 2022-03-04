package org.eol.globi.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import java.io.IOException;
import java.util.Collections;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;

public class CypherQueryExecutorIT {

    @Test
    public void executeQuery() throws IOException {
        CypherQuery query = new CypherQuery("START sourceTaxon = node:taxonPaths('path:\\\"Homo sapiens\\\"') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study-[:IN_DATASET]->dataset RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name SKIP 0 LIMIT 1024");

        String execute = new CypherQueryExecutor(query).execute(null);

        assertThat(execute, containsString("columns"));
        assertNotNull(new ObjectMapper().readTree(execute));
    }

    @Test
    public void executeBoltQuery() {


        Driver driver = GraphDatabase.driver(
                "bolt://preston:7687",
                AuthTokens.none()
        );

        Session session = driver.session(AccessMode.READ);
        try (Transaction transaction = session.beginTransaction()) {

            String s = "CYPHER 2.3 START dataset = node:datasets({namespace}) RETURN dataset.namespace LIMIT 1";
            Statement statement = new Statement(s, new TreeMap<String, Object>() {{
                put("namespace", "namespace:\"globalbioticinteractions/template-dataset\"");
            }});
            StatementResult run = transaction
                    .run(statement);
            run.stream().map(r -> r.asMap()).forEach(System.out::println);
            transaction.success();
        }

    }

}