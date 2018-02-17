package org.eol.globi.server;

import org.eol.globi.domain.InteractType;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.InteractUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.InteractType.*;
import static org.eol.globi.server.CypherQueryBuilder.*;
import static org.eol.globi.server.QueryType.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CypherQueryBuilderTest {

    public static final String EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS = expectedInteractionClause(INTERACTS_WITH);
    public static final String EXPECTED_MATCH_CLAUSE_ALL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true);
    public static final String EXPECTED_MATCH_CLAUSE_DISTINCT = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, false);
    public static final String EXPECTED_MATCH_CLAUSE_SPATIAL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, true, true);
    public static final String EXPECTED_ACCORDING_TO_START_CLAUSE = "START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study ";
    public static final String EXTERNAL_WHERE_CLAUSE_MAMMALIA = "WHERE " + hasTargetTaxon("Mammalia");
    public static final String HAS_TARGET_TAXON_PLANTAE = hasTargetTaxon("Plantae");

    private static String hasTargetTaxon(String taxonName) {
        return hasTaxon(taxonName, "target");
    }

    private static String hasSourceTaxon(String taxonName) {
        return hasTaxon(taxonName, "source");
    }

    private static String hasTaxon(String taxonName, String sourceOrTarget) {
        return "(has(" + sourceOrTarget + "Taxon.externalIds) AND " + sourceOrTarget + "Taxon.externalIds =~ '(.*(" + taxonName + ").*)') ";
    }

    private static String expectedReturnClause() {
        return "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,targetTaxon.name as target_taxon_name,loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.basisOfRecordLabel? as source_specimen_basis_of_record,targetSpecimen.basisOfRecordLabel? as target_specimen_basis_of_record,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalNumberConsumedPercent? as target_specimen_total_count_percent,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.totalVolumePercent? as target_specimen_total_volume_ml_percent,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence,targetSpecimen.frequencyOfOccurrencePercent? as target_specimen_frequency_of_occurrence_percent,loc.footprintWKT? as footprintWKT,loc.locality? as locality";
    }

    private static String expectedInteractionClause(InteractType... interactions) {
        return "sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(interactions) + "]->targetSpecimen";
    }

    private static String expectedMatchClause(String expectedInteractionClause, boolean hasSpatialConstraints, boolean requestedSpatialInfo) {
        String spatialClause = requestedSpatialInfo ? (", sourceSpecimen-[" + (hasSpatialConstraints ? "" : "?") + ":COLLECTED_AT]->loc ") : " ";
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-" + expectedInteractionClause + "-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study" + spatialClause;
    }

    public static final String EXPECTED_RETURN_CLAUSE = "RETURN sourceTaxon.externalId? as source_taxon_external_id," +
            "sourceTaxon.name as source_taxon_name," +
            "sourceTaxon.path? as source_taxon_path," +
            "sourceSpecimen.lifeStageLabel? as source_specimen_life_stage," +
            "sourceSpecimen.basisOfRecordLabel? as source_specimen_basis_of_record," +
            "interaction.label as interaction_type," +
            "targetTaxon.externalId? as target_taxon_external_id," +
            "targetTaxon.name as target_taxon_name," +
            "targetTaxon.path? as target_taxon_path," +
            "targetSpecimen.lifeStageLabel? as target_specimen_life_stage," +
            "targetSpecimen.basisOfRecordLabel? as target_specimen_basis_of_record," +
            "loc.latitude? as latitude," +
            "loc.longitude? as longitude," +
            "study.title as study_title";

    public static final String EXPECTED_RETURN_CLAUSE_DISTINCT = "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
            "RETURN sourceTaxon.externalId? as source_taxon_external_id," +
            "sourceTaxon.name as source_taxon_name," +
            "sourceTaxon.path? as source_taxon_path," +
            "NULL as source_specimen_life_stage," +
            "NULL as source_specimen_basis_of_record," +
            "iType as interaction_type," +
            "targetTaxon.externalId? as target_taxon_external_id," +
            "targetTaxon.name as target_taxon_name," +
            "targetTaxon.path? as target_taxon_path," +
            "NULL as target_specimen_life_stage," +
            "NULL as target_specimen_basis_of_record," +
            "NULL as latitude," +
            "NULL as longitude," +
            "NULL as study_title";

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
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND " + hasTargetTaxon("Arthropoda") +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionTypesForTaxon() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        CypherQuery query = CypherQueryBuilder.buildInteractionTypeQuery(params);

        String concatInteractionTypes = InteractUtil.allInteractionsCypherClause();
        assertThat(query.getQuery(), is("START taxon = node:taxonPaths({taxon_name}) MATCH taxon-[rel:" + concatInteractionTypes + "]->otherTaxon RETURN distinct(type(rel)) as interaction_type"));
        assertThat(query.getParams().toString(), is(is("{taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\"}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinct() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND " + hasTargetTaxon("Arthropoda") +
                EXPECTED_RETURN_CLAUSE_DISTINCT));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnly() throws IOException {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name"});
        }};

        assertInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnly(fieldParams);
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnlyUsingCommaFields() throws IOException {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("fields", new String[]{"source_taxon_name,target_taxon_name"});
        }};

        assertInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnly(fieldParams);
    }

    protected void assertInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnly(Map<String, String[]> fieldParams) {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});

            }
        };
        params.putAll(fieldParams);


        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND " + hasTargetTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithTaxa() throws IOException {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name"});
        }};

        assertFindInteractionsAccordingToWithTaxa(fieldParams);
    }

    @Test
    public void findInteractionsAccordingToWithTaxaWithCommaFields() throws IOException {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("fields", new String[]{"source_taxon_name,target_taxon_name"});
        }};

        assertFindInteractionsAccordingToWithTaxa(fieldParams);
    }

    protected void assertFindInteractionsAccordingToWithTaxa(Map<String, String[]> fieldParams) {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"inaturalist"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("sourceTaxon", new String[]{"Arthropoda"});

            }
        };
        params.putAll(fieldParams);


        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is(EXPECTED_ACCORDING_TO_START_CLAUSE +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE " + hasTargetTaxon("Arthropoda") + "AND " + hasSourceTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E).*, source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"inaturalist"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E).*, target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnly2() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://inaturalist.org/bla"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=(\\\\Qhttp://inaturalist.org/bla\\\\E), target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnlyEmptySource() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://inaturalist.org/bla"});
                put("sourceTaxon", new String[]{""});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=(\\\\Qhttp://inaturalist.org/bla\\\\E), target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is("START targetTaxon = node:taxonPaths({target_taxon_name}) " +
                "MATCH sourceTaxon-[interaction:" + InteractUtil.interactionsCypherClause(INTERACTS_WITH) + "]->targetTaxon " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaOnlyTaxonIdPrefix() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("interactionType", new String[]{"preyedUponBy"});
                put("taxonIdPrefix", new String[]{"somePrefix"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
            put("source_taxon_prefix", "somePrefix.*");
            put("target_taxon_prefix", "somePrefix.*");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractions() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is("START targetTaxon = node:taxonPaths({target_taxon_name}) " +
                "MATCH sourceTaxon-[interaction:" + InteractUtil.interactionsCypherClause(INTERACTS_WITH) + "]->targetTaxon " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"
                ));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void accordingToDataset() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("accordingTo", new String[]{"globi:some/namespace"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions", "number_of_studies", "number_of_sources"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START dataset = node:datasets({accordingTo}) " +
                "MATCH study-[:IN_DATASET]->dataset " +
                "WITH study " +
                "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + createInteractionTypeSelector(Collections.emptyList()) + "]" +
                "->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study " +
                "WHERE (has(targetTaxon.externalIds) AND targetTaxon.externalIds =~ '(.*(Arthropoda).*)') " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon, count(interaction) as interactionCount, count(distinct(id(study))) as studyCount, count(distinct(study.source?)) as sourceCount " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interactionCount as number_of_interactions,studyCount as number_of_studies,sourceCount as number_of_sources"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
            put("accordingTo", "namespace:(some/namespace)");
        }};
        assertThat(query.getParams(), is(expected));
    }


    @Test
    public void findNumberOfStudiesForDistinctInteractionsAccordingTo() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("accordingTo", new String[]{"someSource"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions", "number_of_studies", "number_of_sources"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') " +
                "WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) " +
                "WITH study " +
                "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + createInteractionTypeSelector(Collections.emptyList()) + "]" +
                "->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study " +
                "WHERE (has(targetTaxon.externalIds) AND targetTaxon.externalIds =~ '(.*(Arthropoda).*)') " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon, count(interaction) as interactionCount, count(distinct(id(study))) as studyCount, count(distinct(study.source?)) as sourceCount " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interactionCount as number_of_interactions,studyCount as number_of_studies,sourceCount as number_of_sources"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
            put("accordingTo", ".*(\\\\QsomeSource\\\\E).*");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractionsExcludeChildTaxa() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("excludeChildTaxa", new String[]{"true"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is("START targetTaxon = node:taxons({target_taxon_name}) " +
                "MATCH sourceTaxon-[interaction:" + InteractUtil.interactionsCypherClause(INTERACTS_WITH) + "]->targetTaxon " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "name:\\\"Arthropoda\\\"");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractionsExactNameMatchOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is("START targetTaxon = node:taxons({target_taxon_name}) " +
                "MATCH sourceTaxon-[interaction:" + InteractUtil.interactionsCypherClause(INTERACTS_WITH) + "]->targetTaxon " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "name:\\\"Arthropoda\\\"");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractionsInteractionType() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "interaction_type"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is("START targetTaxon = node:taxonPaths({target_taxon_name}) " +
                "MATCH sourceTaxon-[interaction:" + InteractUtil.interactionsCypherClause(INTERACTS_WITH) + "]->targetTaxon " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.label? as interaction_type"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexInteractionTypeTargetTaxaNumberOfInteractions() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Mammalia"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("interactionType", new String[]{"endoparasiteOf"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                "MATCH sourceTaxon-[interaction:ENDOPARASITE_OF]->targetTaxon " +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"
        ));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("source_taxon_name", "path:\\\"Mammalia\\\"");
            put("target_taxon_name", "path:\\\"Arthropoda\\\"");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnly3() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://inaturalist.org/bla", "http://inaturalist.org/bla2"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=(\\\\Qhttp://inaturalist.org/bla\\\\E|\\\\Qhttp://inaturalist.org/bla2\\\\E), target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"inaturalist"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE " + hasSourceTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E).*, source_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnlyAndExactMatchOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("accordingTo", new String[]{"inaturalist"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE (has(sourceTaxon.name) AND sourceTaxon.name IN ['Arthropoda']) " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E).*, source_taxon_name=name:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnlyAndExcludeChildTaxa() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("excludeChildTaxa", new String[]{"true"});
                put("accordingTo", new String[]{"inaturalist"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE (has(sourceTaxon.name) AND sourceTaxon.name IN ['Arthropoda']) " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E).*, source_taxon_name=name:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnlyAndExactMatchOnlyIncludeObservations() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Insecta"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxons({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (has(targetTaxon.name) AND targetTaxon.name IN ['Insecta'])" +
                " RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=name:\\\"Arthropoda\\\", target_taxon_name=name:\\\"Insecta\\\"}")));
    }

    @Test
    public void prefixSelectorForName() {
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla:123", true), is("externalId:"));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla:123", false), is("path:"));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla name", false), is("path:"));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla name", true), is("name:"));
    }

    ;


    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdOnlyAndExactMatchOnlyIncludeObservations2() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"EOL:123"});
                put("targetTaxon", new String[]{"Insecta"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxons({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (has(targetTaxon.name) AND targetTaxon.name IN ['Insecta'])" +
                " RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=externalId:\\\"EOL:123\\\", target_taxon_name=name:\\\"Insecta\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdAndNamesOnlyAndExactMatchOnlyIncludeObservations2() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"EOL:123", "some name"});
                put("targetTaxon", new String[]{"Insecta"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxons({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (has(targetTaxon.name) AND targetTaxon.name IN ['Insecta'])" +
                " RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=externalId:\\\"EOL:123\\\" OR name:\\\"some name\\\", target_taxon_name=name:\\\"Insecta\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdTargetTaxonIdAndNameOnlyAndExactMatchOnlyIncludeObservations2() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"EOL:123", "some name"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxons({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (has(targetTaxon.name) AND targetTaxon.name IN ['some name'])" +
                " OR (has(targetTaxon.externalId) AND targetTaxon.externalId IN ['EOL:123'])" +
                " RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=name:\\\"Arthropoda\\\", target_taxon_name=externalId:\\\"EOL:123\\\" OR name:\\\"some name\\\"}")));
    }

    @Test
    public void findInteractionsAccordingToNoTaxa() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"inaturalist"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E).*}")));
    }

    @Test
    public void findInteractionsAccordingToMultipleNoTaxa() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"inaturalist", "gomexsi.edu"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=.*(\\\\Qinaturalist\\\\E|\\\\Qgomexsi.edu\\\\E).*}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnlyFlippedFields() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("field", new String[]{"target_taxon_name", "source_taxon_name", "source_taxon_path_ranks"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND " + hasTargetTaxon("Arthropoda") +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN targetTaxon.name as target_taxon_name,sourceTaxon.name as source_taxon_name,sourceTaxon.pathNames? as source_taxon_path_ranks"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findInteractionCountOfEnhydraIncludeObservations() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Enhydra"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"target_taxon_name", "study_citation"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "RETURN targetTaxon.name as target_taxon_name,study.citation? as study_citation"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Enhydra\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void findTaxaAtLocationsDistinct() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') " +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
                "WITH loc " +
                "MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc " +
                "RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctInteractionTypes() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc, taxon-[:" + InteractUtil.interactionsCypherClause(PREYS_UPON, PARASITE_OF) + "]->otherTaxon RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findWithDispersalInteractionType() throws IOException {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Arrays.asList("dispersalVectorOf"));
        assertThat(typeSelector, is("DISPERSAL_VECTOR_OF"));
    }

    @Test
    public void findWithDispersalInteractionType2() throws IOException {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Arrays.asList("hasDispersalVector"));
        assertThat(typeSelector, is("HAS_DISPERAL_VECTOR"));
    }

    @Test
    public void findWithDispersalInteractionTypeParasitoidByInternalName() throws IOException {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Collections.singletonList("HAS_PARASITOID"));
        assertThat(typeSelector, is("HAS_PARASITOID|HAS_HYPERPARASITOID|HAS_ENDOPARASITOID|HAS_ECTOPARASITOID"));
    }

    @Test
    public void findWithDispersalInteractionTypeParasitoidByIRI() throws IOException {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Collections.singletonList("http://purl.obolibrary.org/obo/RO_0002632"));
        assertThat(typeSelector, is("ECTOPARASITE_OF"));
    }

    @Test
    public void findTaxaAtLocationsKillDistinctInteractionTypes() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("interactionType", new String[]{"kills", "parasiteOf"});
            }
        };

        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc, taxon-[:" + InteractUtil.interactionsCypherClause(KILLS, PARASITE_OF) + "]->otherTaxon RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctInteractionTypesSpecificFields() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
                put("field", new String[]{ResultField.TAXON_NAME.getLabel()});
            }
        };

        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc, taxon-[:" + InteractUtil.interactionsCypherClause(PREYS_UPON, PARASITE_OF) + "]->otherTaxon RETURN distinct(taxon.name?) as taxon_name"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctNoSpatialParam() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
            }
        };

        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getQuery(), is("START taxon = node:taxons('*:*') " +
                "RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctNoSpatialInteractTypesParam() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getQuery(), is("START taxon = node:taxons('*:*') MATCH taxon-[:" + InteractUtil.interactionsCypherClause(PREYS_UPON, PARASITE_OF) + "]->otherTaxon RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findInteractionForLocationOnly() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findInteractionForLocationOnlyDistinct() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') " +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
                EXPECTED_RETURN_CLAUSE_DISTINCT));
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
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}"));
    }

    @Test
    public void findInteractions() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{""});
                put("targetTaxon", new String[]{""});
                put("field", new String[]{"source_taxon_name", "interaction_type", "target_taxon_name"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxons('*:*') MATCH sourceTaxon-[interaction:INTERACTS_WITH|PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|IS_HABITAT_OF|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY]->targetTaxon RETURN sourceTaxon.name as source_taxon_name,interaction.label? as interaction_type,targetTaxon.name as target_taxon_name";
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
            }
        };

        String expectedQuery = "START targetTaxon = node:taxonPaths({target_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
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
                expectedMatchClause(expectedInteractionClause(PREYS_UPON, PARASITE_OF), false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyByInteractionTypeDistinct() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                expectedMatchClause(expectedInteractionClause(PREYS_UPON, PARASITE_OF), false, false) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                EXPECTED_RETURN_CLAUSE_DISTINCT;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
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
                expectedMatchClause(expectedInteractionClause(POLLINATED_BY), false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
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
                expectedMatchClause(expectedInteractionClause(SYMBIONT_OF), false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
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
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
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
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Arthropoda\\\", target_taxon_name=path:\\\"Mammalia\\\"}"));
    }

    @Test
    public void findInteractionForAnimaliaAndAnimalia() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Animalia"});
                put("targetTaxon", new String[]{"Animalia"});
                put("interactionType", new String[]{"interactsWith"});
                put("field", new String[]{"source_taxon_name", "source_taxon_external_id", "target_taxon_name", "target_taxon_external_id", "interaction_type", "number_of_interactions"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon-[interaction:INTERACTS_WITH|PREYS_UPON|PARASITE_OF|HAS_HOST|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|IS_HABITAT_OF|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|HYPERPARASITOID_OF|HAS_HYPERPARASITOID|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY]->targetTaxon WHERE (has(targetTaxon.externalIds) AND targetTaxon.externalIds =~ '(.*(Animalia).*)') RETURN sourceTaxon.name as source_taxon_name,sourceTaxon.externalId? as source_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.externalId? as target_taxon_external_id,interaction.label? as interaction_type,interaction.count as number_of_interactions";
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Animalia\\\", target_taxon_name=path:\\\"Animalia\\\"}"));
    }

    @Test
    public void findInteractionForSourceTaxaOnlyNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        String expectedQuery = "START sourceTaxon = node:taxonPaths({source_taxon_name}) " +
                EXPECTED_MATCH_CLAUSE_ALL +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\"}"));
    }

    @Test
    public void findInteractionNoParams() throws IOException {
        String expectedQuery = "START study = node:studies('*:*') " +
                EXPECTED_MATCH_CLAUSE_ALL +
                EXPECTED_RETURN_CLAUSE;
        CypherQuery query = buildInteractionQuery(new HashMap<String, String[]>(), MULTI_TAXON_ALL);
        assertThat(query.getQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{}"));
    }

    @Test
    public void findPreysOnWithLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND " + HAS_TARGET_TAXON_PLANTAE + expectedReturnClause()));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPlantPreyObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE " + HAS_TARGET_TAXON_PLANTAE + expectedReturnClause()));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPlantParasiteObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(PARASITE_OF) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE " + HAS_TARGET_TAXON_PLANTAE + expectedReturnClause()));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPreyObservationsNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", null, params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc " + expectedReturnClause()));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\"}"));
    }

    @Test
    public void findKillsObservationsNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "kills", null, params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(KILLS) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc " + expectedReturnClause()));
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
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND " + HAS_TARGET_TAXON_PLANTAE + "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantPreyWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study WHERE " + HAS_TARGET_TAXON_PLANTAE + "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantParasiteWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + InteractUtil.interactionsCypherClause(PARASITE_OF) + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study WHERE " + HAS_TARGET_TAXON_PLANTAE + "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
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
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE not(has(interact.inverted)) RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
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
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') " +
                "WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
                "WITH loc MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE not(has(interact.inverted)) AND study.source = {source} RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{source=mySource}"));
    }

    @Test
    public void stats() throws IOException {
        CypherQuery query = spatialInfo(null);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') " +
                "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon " +
                "WHERE not(has(interact.inverted)) " +
                "RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, NULL as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{}"));
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
        }}, params, new StringBuilder(), MULTI_TAXON_ALL).toString();
        assertLocationMatchAndWhereClause(clause);
    }

    protected void assertLocationMatchAndWhereClause(String clause) {
        assertThat(clause.replaceAll("\\s+", " ")
                , is(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon" +
                        ", sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc" +
                        " WHERE has(loc.latitude) AND has(loc.longitude) AND loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 "));
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
        }}, params, new StringBuilder(), MULTI_TAXON_ALL).toString();
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
                }}, params, new StringBuilder(), MULTI_TAXON_ALL).toString()
                , is(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon" +
                        ", sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc "));
    }

    @Test
    public void locationsNoConstraints() {
        assertThat(locations().getQuery(), is("START loc = node:locations('latitude:*') RETURN loc.latitude? as latitude, loc.longitude? as longitude, loc.footprintWKT? as footprintWKT"));
    }

    @Test
    public void locationsAccordingTo() {
        Map<String, String> params = new HashMap<String, String>() {
            {
                put("accordingTo", "some source");
            }
        };

        final CypherQuery locationsQuery = locations(params);

        assertThat(locationsQuery.getQuery(), is(EXPECTED_ACCORDING_TO_START_CLAUSE + "MATCH study-[:COLLECTED]->specimen-[:COLLECTED_AT]->location WITH DISTINCT(location) as loc RETURN loc.latitude? as latitude, loc.longitude? as longitude, loc.footprintWKT? as footprintWKT"));
        assertThat(locationsQuery.getParams().toString(), is("{accordingTo=.*(\\\\Qsome source\\\\E).*}"));
    }

    @Test
    public void regexAccordingToGoMexSI() {
        String regex = CypherQueryBuilder.regexForAccordingTo(Arrays.asList("http://gomexsi.tamucc.edu"));
        assertThat(regex, is("(\\\\Qhttp://gomexsi.tamucc.edu\\\\E|\\\\Qhttp://gomexsi.tamucc.edu.\\\\E)"));

        regex = CypherQueryBuilder.regexForAccordingTo(Arrays.asList("http://gomexsi.tamucc.edu", "https://example.com"));
        assertThat(regex, is("(\\\\Qhttp://gomexsi.tamucc.edu\\\\E|\\\\Qhttps://example.com\\\\E|\\\\Qhttp://gomexsi.tamucc.edu.\\\\E)"));

        regex = CypherQueryBuilder.regexForAccordingTo(Arrays.asList("https://example.com"));
        assertThat(regex, is("(\\\\Qhttps://example.com\\\\E)"));
    }

}
