package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InteractionControllerSpringTest extends SpringTestBase {

    @Autowired
    private InteractionController controller;


    @Test
    public void bboxWithDotType() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-82.203,17.077,-79.215,20.632"});
                put("type", new String[]{"dot"});
            }
        });
        CypherQuery interactions = controller.findInteractions(request);

        assertThat(interactions.getVersionedQuery(), Is.is("CYPHER 2.3 START loc = node:locations('latitude:*') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|TROPHICALLY_INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|ACQUIRES_NUTRIENTS_FROM|PROVIDES_NUTRIENTS_FOR|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|CO_OCCURS_WITH|CO_ROOSTS_WITH|HAS_ROOST|ROOST_OF|COMMENSALIST_OF|MUTUALIST_OF|AGGRESSOR_OF|HAS_AGGRESSOR|ALLELOPATH_OF|HAS_ALLELOPATH|HEMIPARASITE_OF|ROOTPARASITE_OF|RELATED_TO]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study-[:IN_DATASET]->dataset, sourceSpecimen-[:COLLECTED_AT]->loc WHERE exists(loc.latitude) AND exists(loc.longitude) AND loc.latitude < 20.632 AND loc.longitude > -82.203 AND loc.latitude > 17.077 AND loc.longitude < -79.215 WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.externalId as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path as source_taxon_path,NULL as source_specimen_occurrence_id,NULL as source_specimen_institution_code,NULL as source_specimen_collection_code,NULL as source_specimen_catalog_number,NULL as source_specimen_life_stage,NULL as source_specimen_basis_of_record,iType as interaction_type,targetTaxon.externalId as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path as target_taxon_path,NULL as target_specimen_occurrence_id,NULL as target_specimen_institution_code,NULL as target_specimen_collection_code,NULL as target_specimen_catalog_number,NULL as target_specimen_life_stage,NULL as target_specimen_basis_of_record,NULL as latitude,NULL as longitude,NULL as study_title SKIP 0 LIMIT 1024"));
        Map<String, String> params = interactions.getParams();
        assertThat(params.toString(), Is.is("{}"));
    }
}