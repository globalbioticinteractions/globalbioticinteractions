package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonSearchImplTest extends Neo4jTestBase {

    @Test
    public void findTaxon() throws IOException {
        CypherQuery query = new TaxonSearchImpl().findTaxonQuery("Apidae");
        validate(query);
        assertThat(query.getVersionedQuery(), Is.is(
                "CYPHER 5 MATCH (taxon:Taxon)-[:SAME_AS*0..1]-(otherTaxon:Taxon) " +
                        "WHERE (taxon.externalIds IS NOT NULL AND taxon.externalIds CONTAINS '| ' + $taxonPathQuery + ' |') " +
                        "AND ((taxon.name IS NOT NULL AND taxon.name = $taxonName) OR (otherTaxon.externalId IS NOT NULL AND otherTaxon.externalId = $taxonName)) " +
                        "AND (((otherTaxon.name IS NOT NULL AND otherTaxon.name = $taxonName) OR (otherTaxon.externalId IS NOT NULL AND otherTaxon.externalId = $taxonName))) " +
                        "RETURN taxon.name as `name`, taxon.commonNames as `commonNames`, taxon.path as `path`, taxon.externalId as `externalId`, taxon.externalUrl as `externalUrl`, taxon.thumbnailUrl as `thumbnailUrl` " +
                        "LIMIT 1"));
        assertThat(query.getParams().toString(), Is.is("{" +
                "taxonName=Apidae, " +
                "taxonPathQuery=Apidae" +
                "}"));
    }

    @Test
    public void findTaxonWithImage() throws IOException {
        CypherQuery query = new TaxonSearchImpl().findTaxonWithImageQuery("Apidae");
        validate(query);
        assertThat(query.getVersionedQuery(), Is.is(
                "CYPHER 5 MATCH (taxon:Taxon)-[:SAME_AS*0..1]-(otherTaxon:Taxon) " +
                        "WHERE (taxon.externalIds IS NOT NULL AND taxon.externalIds CONTAINS '| ' + $taxonPathQuery + ' |') " +
                        "AND ((taxon.name IS NOT NULL AND taxon.name = $taxonName) OR (otherTaxon.externalId IS NOT NULL " +
                        "AND otherTaxon.externalId = $taxonName)) " +
                        "AND (((otherTaxon.name IS NOT NULL AND otherTaxon.name = $taxonName) OR (otherTaxon.externalId IS NOT NULL AND otherTaxon.externalId = $taxonName))) " +
                        "AND taxon.thumbnailUrl IS NOT NULL AND NOT isEmpty(taxon.thumbnailUrl) " +
                        "RETURN taxon.name as `name`, taxon.commonNames as `commonNames`, taxon.path as `path`, taxon.externalId as `externalId`, taxon.externalUrl as `externalUrl`, taxon.thumbnailUrl as `thumbnailUrl` " +
                        "LIMIT 1"));
        assertThat(query.getParams().toString(), Is.is("{" +
                "taxonName=Apidae, " +
                "taxonPathQuery=Apidae" +
                "}"));
    }

    @Test
    public void findCloseMatchesFamily() throws IOException {
        CypherQuery query = new TaxonSearchImpl().findCloseMatches("Apidea", null);
        validate(query);
        assertThat(query.getVersionedQuery(), Is.is(
                "CYPHER 5 MATCH (taxon:Taxon {name: $taxonName}) " +
                        "RETURN taxon.name as taxon_name,taxon.commonNames as taxon_common_names,taxon.path as taxon_path,taxon.pathIds as taxon_path_ids " +
                        "LIMIT 1 " +
                        "UNION CALL db.index.fulltext.queryNodes('taxonNameSuggestions', '(name:apidea* OR name:apidea~)') " +
                        "YIELD node as taxon " +
                        "RETURN taxon.name as taxon_name,taxon.commonNames as taxon_common_names,taxon.path as taxon_path,taxon.pathIds as taxon_path_ids " +
                        "SKIP 0 LIMIT 30"));
        assertThat(query.getParams().toString(), Is.is("{taxonName=Apidea}"));
    }

    @Test
    public void escapeLuceneTerms() {
        String query = TaxonSearchImpl.buildLuceneQuery("Sphagnum fallax)Sphagnum fallax)", "name");

        assertThat(query, Is.is("(name:sphagnum* OR name:sphagnum~) AND (name:fallax\\)sphagnum* OR name:fallax\\)sphagnum~) AND (name:fallax\\)* OR name:fallax\\)~)"));
    }

    @Test
    public void findCloseMatchesNameWithParenthesis() throws IOException {
        CypherQuery query = new TaxonSearchImpl().findCloseMatches("Sphagnum fallax)Sphagnum fallax)", null);
        validate(query);
        assertThat(query.getVersionedQuery(), Is.is(
                "CYPHER 5 MATCH (taxon:Taxon {name: $taxonName}) " +
                        "RETURN taxon.name as taxon_name,taxon.commonNames as taxon_common_names,taxon.path as taxon_path,taxon.pathIds as taxon_path_ids " +
                        "LIMIT 1 " +
                        "UNION CALL db.index.fulltext.queryNodes('taxonNameSuggestions', '(name:sphagnum* OR name:sphagnum~) AND (name:fallax\\)sphagnum* OR name:fallax\\)sphagnum~) AND (name:fallax\\)* OR name:fallax\\)~)') " +
                        "YIELD node as taxon " +
                        "RETURN taxon.name as taxon_name,taxon.commonNames as taxon_common_names,taxon.path as taxon_path,taxon.pathIds as taxon_path_ids " +
                        "SKIP 0 LIMIT 30"));
        assertThat(query.getParams().toString(), Is.is("{taxonName=Sphagnum fallax)Sphagnum fallax)}"));
    }

    @Test
    public void findCloseMatchesSpecies() throws IOException {
        CypherQuery query = new TaxonSearchImpl().findCloseMatches("Apiz mellifera", null);
        validate(query);
        assertThat(query.getVersionedQuery(), Is.is(
                "CYPHER 5 MATCH (taxon:Taxon {name: $taxonName}) " +
                        "RETURN taxon.name as taxon_name,taxon.commonNames as taxon_common_names,taxon.path as taxon_path,taxon.pathIds as taxon_path_ids " +
                        "LIMIT 1 " +
                        "UNION " +
                        "CALL db.index.fulltext.queryNodes('taxonNameSuggestions', '(name:apiz* OR name:apiz~) AND (name:mellifera* OR name:mellifera~)') " +
                        "YIELD node as taxon " +
                        "RETURN taxon.name as taxon_name,taxon.commonNames as taxon_common_names,taxon.path as taxon_path,taxon.pathIds as taxon_path_ids " +
                        "SKIP 0 " +
                        "LIMIT 30"
        ));
        assertThat(query.getParams().toString(), Is.is("{taxonName=Apiz mellifera}"));
    }


}