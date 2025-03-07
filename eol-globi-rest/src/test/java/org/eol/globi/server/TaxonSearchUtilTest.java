package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.HashMap;

import static org.eol.globi.server.CypherQueryBuilderTest.getNeo4jRule;
import static org.hamcrest.MatcherAssert.assertThat;
public class TaxonSearchUtilTest {

    @Rule
    public Neo4jRule neo4j = getNeo4jRule();

    @Test
    public void createQuery() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("Animalia", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 2.3 START someTaxon = node:taxons({pathQuery}) MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE exists(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=name:\"Animalia\"}"));
        validate(query);
    }

    public void validate(CypherQuery query) {
        CypherTestUtil.validate(query, neo4j.getGraphDatabaseService());
    }

    @Test
    public void createQueryWithSupportedId() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("EOL:123", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 2.3 START someTaxon = node:taxons({pathQuery}) MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE exists(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=externalId:\"EOL:123\"}"));
        validate(query);
    }

    @Test
    public void createQueryWithSupportedId2() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("http://taxon-concept.plazi.org/id/Animalia/Caridae_Dana_1852", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 2.3 START someTaxon = node:taxons({pathQuery}) MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE exists(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=externalId:\"http://taxon-concept.plazi.org/id/Animalia/Caridae_Dana_1852\"}"));
        validate(query);
    }

    @Test
    public void createQueryWithUnsupportedId() {
        CypherQuery query = TaxonSearchUtil.getCypherQuery("FOO:1", new HashMap());
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 2.3 START someTaxon = node:taxons({pathQuery}) MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE exists(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=name:\"FOO:1\"}"));
        validate(query);
    }

    @Test
    public void linksForTaxonName() {
        CypherQuery query = TaxonSearchUtil.createPagedQuery("Enhydra lutris", null);
        assertThat(query.getVersionedQuery(), Is.is("CYPHER 2.3 START someTaxon = node:taxons({pathQuery}) MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE exists(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl RETURN externalId as taxon_external_id,externalUrl as taxon_external_url SKIP 0 LIMIT 30"));
        assertThat(query.getParams().toString(), Is.is("{pathQuery=name:\"Enhydra lutris\"}"));
        validate(query);
    }

}