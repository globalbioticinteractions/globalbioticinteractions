package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonSearchImplTest extends Neo4jTestBase {


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