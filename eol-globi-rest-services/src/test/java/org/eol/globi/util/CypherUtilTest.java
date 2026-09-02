package org.eol.globi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CypherUtilTest  {

    @Test
    public void escapeQueryWithFunnyCharacters() throws JsonProcessingException {
        CypherQuery cypherQuery = new CypherQuery("CYPHER 5 MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:HAS_ECTOPARASITE]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon),(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['Megistopoda aranea (Coquillétt, 1899)']) AND (sourceTaxon.externalId IS NOT NULL AND sourceTaxon.externalId IN ['COL:852KK']) OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) RETURN study.title");
        String json = CypherUtil.toJson(cypherQuery);
        JsonNode jsonNode = new ObjectMapper().readTree(json);
        assertThat(jsonNode.has("statements"), Is.is(true));
        assertThat(jsonNode.get("statements").get(0).get("statement").asText(),
                containsString("Megistopoda aranea (Coquill\\u00E9tt, 1899)"));
    }

}