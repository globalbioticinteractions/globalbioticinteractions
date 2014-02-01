package org.eol.globi.server;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CypherQueryBuilderTest {

    @Test
    public void findInteractionForSourceAndTargetTaxaLocations() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        String expectedQuery = "START loc = node:locations('*:*') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc is not null AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(sourceTaxon.path) AND sourceTaxon.path =~ '(.*(Actinopterygii|Chordata).*)' AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' RETURN sourceTaxon.externalId? as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path? as source_taxon_path,type(interactionType) as interaction_type,targetTaxon.externalId? as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path? as target_taxon_path";
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(params);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams(), is(nullValue()));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonpaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' RETURN sourceTaxon.externalId? as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path? as source_taxon_path,type(interactionType) as interaction_type,targetTaxon.externalId? as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path? as target_taxon_path";
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(params);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
            }
        };

        String expectedQuery = "START targetTaxon = node:taxonpaths({target_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon RETURN sourceTaxon.externalId? as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path? as source_taxon_path,type(interactionType) as interaction_type,targetTaxon.externalId? as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path? as target_taxon_path";
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(params);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{target_taxon_name=path:\\\"Arthropoda\\\"}"));
    }

    @Test
    public void findInteractionForSourceTaxaOnlyNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonpaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon RETURN sourceTaxon.externalId? as source_taxon_external_id,sourceTaxon.name as source_taxon_name,sourceTaxon.path? as source_taxon_path,type(interactionType) as interaction_type,targetTaxon.externalId? as target_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.path? as target_taxon_path";
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(params);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\"}"));
    }

    @Test
    public void findPreysOnWithLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = CypherQueryBuilder.interactionObservations("Homo sapiens", "preysOn", "Plantae", params);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonpaths({source_taxon_name}), targetTaxon = node:taxonpaths({target_taxon_name}) MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:ATE|PREYS_UPON]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon),(sourceSpecimen)<-[collected_rel:COLLECTED]-(study), sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc is not null AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,targetTaxon.name as target_taxon_name, loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
    }

    @Test
    public void findPlantPreyObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = CypherQueryBuilder.interactionObservations("Homo sapiens", "preysOn", "Plantae", params);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonpaths({source_taxon_name}), targetTaxon = node:taxonpaths({target_taxon_name}) MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:ATE|PREYS_UPON]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon),(sourceSpecimen)<-[collected_rel:COLLECTED]-(study) RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,targetTaxon.name as target_taxon_name, null as latitude, null as longitude, null as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPreyObservationsNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = CypherQueryBuilder.interactionObservations("Homo sapiens", "preysOn", null, params);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonpaths({source_taxon_name}) MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:ATE|PREYS_UPON]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon),(sourceSpecimen)<-[collected_rel:COLLECTED]-(study) RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,targetTaxon.name as target_taxon_name, null as latitude, null as longitude, null as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\"}"));
    }

    @Test
    public void findDistinctPreyWithLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = CypherQueryBuilder.distinctInteractions("Homo sapiens", "preysOn", "Plantae", params);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonpaths({source_taxon_name}), targetTaxon = node:taxonpaths({target_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc is not null AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 RETURN sourceTaxon.name as source_taxon_name, 'preysOn' as interaction_type, collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantPreyWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = CypherQueryBuilder.distinctInteractions("Homo sapiens", "preysOn", "Plantae", params);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonpaths({source_taxon_name}), targetTaxon = node:taxonpaths({target_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon RETURN sourceTaxon.name as source_taxon_name, 'preysOn' as interaction_type, collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }



}
