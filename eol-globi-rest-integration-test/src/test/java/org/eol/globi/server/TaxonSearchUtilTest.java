package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.core.Is;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
public class TaxonSearchUtilTest extends Neo4jTestBase {


    @Test
    public void createQuery() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("Animalia", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is(
                "CYPHER 5 " +
                        "MATCH (someTaxon:Taxon { name: $pathQuery })-[:SAME_AS*0..1]->(taxon:Taxon) " +
                        "WHERE taxon.externalId IS NOT NULL " +
                        "WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl " +
                        "RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=Animalia}"));
        validate(query);
    }

    @Test
    public void createQueryWithSupportedId() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("EOL:123", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 5 " +
                "MATCH (someTaxon:Taxon { externalId: $pathQuery })-[:SAME_AS*0..1]->(taxon:Taxon) " +
                "WHERE taxon.externalId IS NOT NULL " +
                "WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl " +
                "RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=EOL:123}"));
        validate(query);
    }

    @Test
    public void createQueryWithSupportedId2() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("http://taxon-concept.plazi.org/id/Animalia/Caridae_Dana_1852", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 5 MATCH (someTaxon:Taxon { externalId: $pathQuery })-[:SAME_AS*0..1]->(taxon:Taxon) WHERE taxon.externalId IS NOT NULL WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=http://taxon-concept.plazi.org/id/Animalia/Caridae_Dana_1852}"));
        validate(query);
    }

    @Test
    public void createQueryWithUnsupportedId() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("FOO:1", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 5 MATCH (someTaxon:Taxon { externalId: $pathQuery })-[:SAME_AS*0..1]->(taxon:Taxon) WHERE taxon.externalId IS NOT NULL WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=FOO:1}"));
        validate(query);
    }

    @Test
    public void linksForTaxonName() {
        CypherQuery query = TaxonSearchUtil.createPagedQuery("Enhydra lutris", null);
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 5 MATCH (someTaxon:Taxon { name: $pathQuery })-[:SAME_AS*0..1]->(taxon:Taxon) WHERE taxon.externalId IS NOT NULL WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url SKIP 0 LIMIT 30"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=Enhydra lutris}"));
        validate(query);
    }

}