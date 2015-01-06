package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.InteractUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.server.CypherQueryBuilder.QueryType.MULTI_TAXON_ALL;
import static org.eol.globi.server.CypherQueryBuilder.QueryType.SINGLE_TAXON_ALL;
import static org.eol.globi.server.CypherQueryBuilder.QueryType.SINGLE_TAXON_DISTINCT;
import static org.eol.globi.server.CypherQueryBuilder.appendMatchAndWhereClause;
import static org.eol.globi.server.CypherQueryBuilder.appendStartClause;
import static org.eol.globi.server.CypherQueryBuilder.buildInteractionQuery;
import static org.eol.globi.server.CypherQueryBuilder.spatialInfo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CypherQueryBuilderTest {

    public static final String EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS = expectedInteractionClause(InteractUtil.allInteractionsCypherClause());
    public static final String EXPECTED_MATCH_CLAUSE = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false);
    public static final String EXPECTED_MATCH_CLAUSE_SPATIAL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, true);

    private static String expectedInteractionClause(String interactions) {
        return "sourceSpecimen-[interactionType:" + interactions + "]->targetSpecimen";
    }

    private static String expectedMatchClause(String expectedInteractionClause, boolean isSpatial) {
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-" + expectedInteractionClause + "-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[" + (isSpatial ? "" : "?") + ":COLLECTED_AT]->loc ";
    }

    public static final String EXPECTED_RETURN_CLAUSE = "RETURN sourceTaxon.externalId? as source_taxon_external_id," +
            "sourceTaxon.name as source_taxon_name," +
            "sourceTaxon.path? as source_taxon_path," +
            "sourceSpecimen.lifeStage? as source_specimen_life_stage," +
            "type(interactionType) as interaction_type," +
            "targetTaxon.externalId? as target_taxon_external_id," +
            "targetTaxon.name as target_taxon_name," +
            "targetTaxon.path? as target_taxon_path," +
            "targetSpecimen.lifeStage? as target_specimen_life_stage," +
            "loc.latitude? as latitude," +
            "loc.longitude? as longitude," +
            "study.title as study_title";

    @Test
    public void findInteractionForSourceAndTargetTaxaLocations() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionForLocationOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START loc = node:locations('*:*') " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
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

        String expectedQuery = "START targetTaxon = node:taxonPaths({target_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{target_taxon_name=path:\\\"Arthropoda\\\"}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyByInteractionType() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                expectedMatchClause(expectedInteractionClause("ATE|PREYS_UPON|PARASITE_OF|HAS_HOST"), false) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }


    @Test
    public void findInteractionForTargetTaxaPollinates() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"pollinatedBy"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                expectedMatchClause(expectedInteractionClause("POLLINATED_BY"), false) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }

    @Test
    public void findSymbioticInteractions() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"symbiontOf"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }

    @Test
    public void findAnyInteractions() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"interactsWith"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInteractionForTargetTaxaOnlyByInteractionTypeNotSupported() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"mickeyMouse"});
            }
        };

        buildInteractionQuery(params, MULTI_TAXON_ALL);
    }

    @Test
    public void findInteractionForTargetTaxaOnlyByEmptyInteractionTypeNotSupported() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{""});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }

    @Test
    public void findInteractionForSourceTaxaOnlyNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\"}"));
    }

    @Test
    public void findInteractionNoParams() throws IOException {
        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(new HashMap<String, String[]>(), MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\"}"));
    }

    @Test
    public void findPreysOnWithLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,targetTaxon.name as target_taxon_name, loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPlantPreyObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,targetTaxon.name as target_taxon_name, loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPlantParasiteObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PARASITE_OF|HAS_HOST]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name,'parasiteOf' as interaction_type,targetTaxon.name as target_taxon_name, loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPreyObservationsNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", null, params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,targetTaxon.name as target_taxon_name, loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\"}"));
    }

    @Test
    public void findDistinctPreyWithLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name, 'preysOn' as interaction_type, collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantPreyWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name, 'preysOn' as interaction_type, collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantParasiteWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:PARASITE_OF|HAS_HOST]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name, 'parasiteOf' as interaction_type, collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void statsWithBBox() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = spatialInfo(params);
        assertThat(query.getQuery(), is("START loc = node:locations('*:*') WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE not(has(interact.inverted)) RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{}"));
    }

    @Test
    public void statsWithBBoxAndSource() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("source", new String[]{"mySource"});
            }
        };

        CypherQuery query = spatialInfo(params);
        assertThat(query.getQuery(), is("START loc = node:locations('*:*') WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE not(has(interact.inverted)) AND study.source = {source} RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{source=mySource}"));
    }

    @Test
    public void stats() throws IOException {
        CypherQuery query = spatialInfo(null);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon WHERE not(has(interact.inverted)) RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, NULL as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{}"));
    }


    @Test
    public void interactionStartClauseSourceTaxonOnly() {
        assertThat(appendStartClause(true, false, new StringBuilder()).toString()
                , is("START sourceTaxon = node:taxonPaths({source_taxon_name})"));
    }

    @Test
    public void interactionStartClauseTargetTaxonOnly() {
        assertThat(appendStartClause(false, true, new StringBuilder()).toString()
                , is("START targetTaxon = node:taxonPaths({target_taxon_name})"));
    }

    @Test
    public void interactionStartClauseSourceAndTarget() {
        assertThat(appendStartClause(true, true, new StringBuilder()).toString()
                , is("START sourceTaxon = node:taxonPaths({source_taxon_name}), targetTaxon = node:taxonPaths({target_taxon_name})"));
    }

    @Test
    public void interactionMatchClauseWithLocationPreysOn() {
        Map<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };
        String clause = appendMatchAndWhereClause(new ArrayList<String>() {{
            add("preysOn");
        }}, params, new StringBuilder()).toString();
        assertLocationMatchAndWhereClause(clause);
    }

    protected void assertLocationMatchAndWhereClause(String clause) {
        assertThat(clause
                , is(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon" +
                ", sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc" +
                " WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 "));
    }

    @Test
    public void interactionMatchClauseWithLocationPreysOn2() {
        Map<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };
        String clause = appendMatchAndWhereClause(new ArrayList<String>() {{
            add("preysOn");
        }}, params, new StringBuilder()).toString();
        assertLocationMatchAndWhereClause(clause);
    }

    @Test
    public void interactionMatchClauseWithoutLocationPreysOn() {
        Map<String, String[]> params = new HashMap<String, String[]>() {
            {
            }
        };

        assertThat(appendMatchAndWhereClause(new ArrayList<String>() {{
            add("preysOn");
        }}, params, new StringBuilder()).toString()
                , is(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:ATE|PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon" +
                ", sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc "));
    }

}
