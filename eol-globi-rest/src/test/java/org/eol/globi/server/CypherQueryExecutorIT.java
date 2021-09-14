package org.eol.globi.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.CypherQuery;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;

public class CypherQueryExecutorIT {

    @Test
    public void executeQuery() throws IOException {
        CypherQuery query = new CypherQuery("CYPHER 2.3 START sourceTaxon = node:taxonPaths('path:\\\"Homo sapiens\\\"') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study-[:IN_DATASET]->dataset RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name SKIP 0 LIMIT 1024");

        String execute = new CypherQueryExecutor(query).execute(null);

        assertThat(execute, containsString("columns"));
        assertNotNull(new ObjectMapper().readTree(execute));
    }

}