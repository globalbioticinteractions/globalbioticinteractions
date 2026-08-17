package org.eol.globi.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.CypherQuery;
import org.junit.Test;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;

public class CypherQueryExecutorHTTPIT {

    @Test
    public void executeQuery() throws IOException {
        CypherQuery query = new CypherQuery(cypherQuery(), cypherQueryParams());

        String execute = new CypherQueryExecutorHTTP(query).execute(null);

        assertThat(execute, containsString("columns"));
        assertNotNull(new ObjectMapper().readTree(execute));
    }

    @Test
    public void executeBoltQuery() {
        try (Driver driver1 = getDriver()) {
            try (Session session = driver1.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build())) {
                try (Transaction transaction = session.beginTransaction()) {
                    String s = cypherQuery();
                    TreeMap<String, Object> params = new TreeMap<String, Object>() {{
                        putAll(cypherQueryParams());
                    }};
                    Result run = transaction.run(s, params);
                    List<String> collect = run.stream()
                            .map(r -> r.get(0).asString())
                            .collect(Collectors.toList());
                    assertThat(collect, hasItem("globalbioticinteractions/template-dataset"));
                    transaction.commit();
                }

            }
        }

    }

    private static Driver getDriver() {
        return GraphDatabase.driver(
                "bolt://localhost:7687",
                AuthTokens.none()
        );
    }

    @Test
    public void executeBoltQuery2() {


        try (Driver driver1 = getDriver()) {
            try (Session session = driver1.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build())) {
                try (Transaction transaction = session.beginTransaction()) {
                    String s = "MATCH p = allShortestPaths((startNode:Taxon {name: $beginNode})-[:PREYS_UPON|PARASITE_OF|HAS_HOST|HAS_RESERVOIR_HOST|INTERACTS_WITH|TROPHICALLY_INTERACTS_WITH|HOST_OF|RESERVOIR_HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|ACQUIRES_NUTRIENTS_FROM|PROVIDES_NUTRIENTS_FOR|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|CO_OCCURS_WITH|CO_ROOSTS_WITH|HAS_ROOST|ROOST_OF|COMMENSALIST_OF|MUTUALIST_OF|AGGRESSOR_OF|HAS_AGGRESSOR|ALLELOPATH_OF|HAS_ALLELOPATH|HEMIPARASITE_OF|ROOTPARASITE_OF|HAS_ECTOMYCORRHIZAL_HOST|ECTOMYCORRHIZAL_HOST_OF|HAS_ARBUSCULAR_MYCORRHIZAL_HOST|ARBUSCULAR_MYCORRHIZAL_HOST_OF|RELATED_TO|CLASSIFIED_AS*..100]-(endNode:Taxon {name: $endNode})) WITH p LIMIT 1 UNWIND nodes(p) as n CALL (n) { MATCH(n:Taxon) RETURN n.name as name} RETURN name SKIP 0 LIMIT 1024";
                    TreeMap<String, Object> params = new TreeMap<String, Object>() {{
                        putAll(new TreeMap<String, String>() {{
                            put("beginNode", "Apodemus mystacinus");
                            put("endNode", "Paragoniocotes tenuigaster");
                        }});
                    }};
                    Result run = transaction.run(s, params);
                    List<String> collect = run.stream()
                            .map(r -> r.get(0).asString())
                            .collect(Collectors.toList());
                    assertThat(collect, hasItem("Apodemus mystacinus"));
                    assertThat(collect, hasItem("Paragoniocotes tenuigaster"));
                    transaction.commit();
                }

            }
        }

    }


    private static TreeMap<String, String> cypherQueryParams() {
        return new TreeMap<String, String>() {{
            put("namespace", "globalbioticinteractions/template-dataset");
        }};
    }

    private static String cypherQuery() {
        return "MATCH (dataset:Dataset {namespace: $namespace}) RETURN dataset.namespace LIMIT 1";
    }

}