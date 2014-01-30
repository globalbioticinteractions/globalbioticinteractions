package org.eol.globi.server;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CypherQueryBuilderTest {

    @Test
    public void findInteractionForSourceAndTargetTaxa() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Mammalia"});
                put("targetTaxon", new String[]{"Reptilia"});
                put("nw_lat", new String[]{"18.34"});
                put("nw_lng", new String[]{"-66.50"});
                put("se_lat", new String[]{"18.14"});
                put("se_lng", new String[]{"-66.48"});
            }
        };
        CypherQuery query = new CypherQueryBuilder().buildInteractionQuery(params);
        String expectedQuery = "START loc = node:locations('*:*') , sourceTaxon = node:taxonpaths('path:\\\"Mammalia\\\"'), targetTaxon = node:taxonpaths('path:\\\"Reptilia\\\"') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon  , sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc is not null AND loc.latitude < 18.34 AND loc.longitude > -66.5 AND loc.latitude > 18.14 AND loc.longitude < -66.48 RETURN sourceTaxon.externalId? as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path? as source_taxon_path,type(interactionType) as interaction_type,targetTaxon.externalId? as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path? as target_taxon_path " + CypherQueryBuilder.DEFAULT_LIMIT_CLAUSE;
        assertThat(query.getQuery(), is(expectedQuery));
    }

}
