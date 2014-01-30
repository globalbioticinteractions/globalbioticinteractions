package org.eol.globi.server;

import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CypherQueryBuilderTest {

    @Test
    public void findInteractionForSourceAndTargetTaxa() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii","Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        String expectedQuery = "START loc = node:locations('*:*')  MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon  , sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc is not null AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08  AND has(sourceTaxon.path) AND sourceTaxon.path =~ '(.*(Actinopterygii|Chordata).*)'  AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' RETURN sourceTaxon.externalId? as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path? as source_taxon_path,type(interactionType) as interaction_type,targetTaxon.externalId? as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path? as target_taxon_path " + CypherQueryBuilder.DEFAULT_LIMIT_CLAUSE;
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(params);
        assertThat(query.getQuery(), is(expectedQuery));
    }

}
