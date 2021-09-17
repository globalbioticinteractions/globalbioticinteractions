package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.http.HttpResponse;
import org.eol.globi.server.util.RequestHelper;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertFalse;

public class CypherUtilIT {

    @Test
    public void execute() throws IOException {
        String query = "START study = node:studies('*:*') " +
                "MATCH study-[:IN_DATASET]->dataset " +
                "WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(dataset.citation) AND dataset.citation =~ {accordingTo}) " +
                "WITH study " +
                "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:INTERACTS_WITH|PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|COMMENSALIST_OF]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, " +
                "sourceSpecimen<-[collected_rel:COLLECTED]-study-[:IN_DATASET]->dataset " +
                "OPTIONAL MATCH sourceSpecimen-[:COLLECTED_AT]->loc " +
                "RETURN sourceTaxon.externalId as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path as source_taxon_path,sourceSpecimen.lifeStageLabel as source_specimen_life_stage,sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record,interaction.label as interaction_type,targetTaxon.externalId as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path as target_taxon_path,targetSpecimen.lifeStageLabel as target_specimen_life_stage,targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record,loc.latitude as latitude,loc.longitude as longitude,study.title as study_title";
        HashMap<String, String> params = new HashMap<String, String>() {{
            put("accordingTo", "(?i).*(\\\\Qgomexsi\\\\E).*");
        }};
        HttpResponse execute = CypherUtil.execute(new CypherQuery(query, params, CypherUtil.CYPHER_VERSION_2_3));
        ProxyOutputStream proxyOutputStream = new ProxyOutputStream(new NullOutputStream()) {
            AtomicLong count = new AtomicLong(0L);
            @Override
            protected void beforeWrite(final int n) throws IOException {
                long l = count.incrementAndGet();
                if (l % 1024 == 0) {
                    System.out.println(".");
                }
            }
        };

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(execute.getEntity().getContent(), os);
        proxyOutputStream.flush();
        proxyOutputStream.close();
    }

    @Test
    public void nonEmptyData() throws IOException {
        String query = "START study = node:studies('*:*') " +
                "MATCH study-[:IN_DATASET]->dataset " +
                "WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) " +
                "OR (has(study.citation) AND study.citation =~ {accordingTo}) " +
                "OR (has(dataset.citation) AND dataset.citation =~ {accordingTo}) " +
                "WITH study " +
                "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:INTERACTS_WITH|PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|COMMENSALIST_OF]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, " +
                "sourceSpecimen<-[collected_rel:COLLECTED]-study " +
                "OPTIONAL MATCH sourceSpecimen-[:COLLECTED_AT]->loc " +
                "RETURN sourceTaxon.externalId as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path as source_taxon_path,sourceSpecimen.lifeStageLabel as source_specimen_life_stage,sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record,interaction.label as interaction_type,targetTaxon.externalId as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path as target_taxon_path,targetSpecimen.lifeStageLabel as target_specimen_life_stage,targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record,loc.latitude as latitude,loc.longitude as longitude,study.title as study_title " +
                "LIMIT 1";
        HashMap<String, String> params = new HashMap<String, String>() {{
            put("accordingTo", "http://arctos.database.museum/guid/MSB:Para:1678");
        }};
        HttpResponse execute = CypherUtil.execute(new CypherQuery(query, params, CypherUtil.CYPHER_VERSION_2_3));

        String body = IOUtils.toString(execute.getEntity().getContent(), StandardCharsets.UTF_8);
        assertFalse(RequestHelper.emptyData(body));
        assertThat(body, containsString("http://arctos.database.museum/guid/MSB:Para:1678"));
    }

    @Test
    public void emptyData() throws IOException {
        String query = "START study = node:studies('*:*') " +
                "MATCH study-[:IN_DATASET]->dataset " +
                "WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) " +
                "OR (has(study.citation) AND study.citation =~ {accordingTo}) " +
                "OR (has(dataset.citation) AND dataset.citation =~ {accordingTo}) " +
                "WITH study " +
                "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:INTERACTS_WITH|PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|COMMENSALIST_OF]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, " +
                "sourceSpecimen<-[collected_rel:COLLECTED]-study-[:IN_DATASET]->dataset " +
                "OPTIONAL MATCH sourceSpecimen-[:COLLECTED_AT]->loc " +
                "RETURN sourceTaxon.externalId as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path as source_taxon_path,sourceSpecimen.lifeStageLabel as source_specimen_life_stage,sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record,interaction.label as interaction_type,targetTaxon.externalId as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path as target_taxon_path,targetSpecimen.lifeStageLabel as target_specimen_life_stage,targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record,loc.latitude as latitude,loc.longitude as longitude,study.title as study_title " +
                "LIMIT 1";
        HashMap<String, String> params = new HashMap<String, String>() {{
            put("accordingTo", "http://arctos.database.museum/guid/MSB:Para:1678999999999999");
        }};
        HttpResponse execute = CypherUtil.execute(new CypherQuery(query, params, CypherUtil.CYPHER_VERSION_2_3));

        String body = IOUtils.toString(execute.getEntity().getContent(), StandardCharsets.UTF_8);
        assertThat(RequestHelper.emptyData(body), Is.is(true));
    }


}