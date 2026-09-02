package org.eol.globi.server;

import org.eol.globi.domain.InteractType;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.InteractUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.QueryExecutionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.domain.InteractType.INTERACTS_WITH;
import static org.eol.globi.domain.InteractType.KILLS;
import static org.eol.globi.domain.InteractType.PARASITE_OF;
import static org.eol.globi.domain.InteractType.POLLINATED_BY;
import static org.eol.globi.domain.InteractType.PREYS_UPON;
import static org.eol.globi.domain.InteractType.RELATED_TO;
import static org.eol.globi.domain.InteractType.SYMBIONT_OF;
import static org.eol.globi.server.CypherQueryBuilder.appendMatchAndWhereClause;
import static org.eol.globi.server.CypherQueryBuilder.buildInteractionQuery;
import static org.eol.globi.server.CypherQueryBuilder.createInteractionTypeSelector;
import static org.eol.globi.server.CypherQueryBuilder.locations;
import static org.eol.globi.server.CypherQueryBuilder.spatialInfo;
import static org.eol.globi.server.QueryType.MULTI_TAXON_ALL;
import static org.eol.globi.server.QueryType.MULTI_TAXON_DISTINCT;
import static org.eol.globi.server.QueryType.MULTI_TAXON_DISTINCT_BY_NAME_ONLY;
import static org.eol.globi.server.QueryType.SINGLE_TAXON_ALL;
import static org.eol.globi.server.QueryType.SINGLE_TAXON_DISTINCT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.fail;

public class CypherQueryBuilderTest extends Neo4jTestBase {

    private static final String EXPECTED_CYPHER_VERSION = "CYPHER 5 ";

    private static final String EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS = expectedInteractionClause(RELATED_TO);
    private static final String EXPECTED_MATCH_CLAUSE_ALL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true);
    private static final String EXPECTED_MATCH_CLAUSE_DISTINCT = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, false);
    private static final String EXPECTED_MATCH_CLAUSE_DISTINCT_REFUTING = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, false, true);
    private static final String EXPECTED_MATCH_CLAUSE_DISTINCT_REFUTING_AND_SUPPORTING = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, false, "REFUTES|COLLECTED");
    private static final String EXPECTED_MATCH_CLAUSE_SPATIAL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, true, true);
    private static final String EXPECTED_ACCORDING_TO_START_CLAUSE = EXPECTED_CYPHER_VERSION +
            "MATCH (x:Reference)-[:IN_DATASET|HAS_DOI|HAS_EXTERNAL_ID*]->(externalId:ExternalId {externalId: $accordingTo})" +
            " WITH x as study ";
    private static final String EXTERNAL_WHERE_CLAUSE_MAMMALIA = "WHERE " + hasTargetTaxon("Mammalia");
    private static final String HAS_TARGET_TAXON_PLANTAE = hasTargetTaxon("Plantae");

    private static String hasTargetTaxon(String taxonName) {
        return hasTaxon(taxonName, "target");
    }

    private static String hasTaxon(String taxonName, String sourceOrTarget) {
        return "(" + sourceOrTarget + "Taxon.externalIds IS NOT NULL AND (" + sourceOrTarget + "Taxon.externalIds CONTAINS '| ' + '" + taxonName + "' + ' |')) ";
    }

    private static String expectedReturnClause() {
        return "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,targetTaxon.name as target_taxon_name,loc.latitude as latitude,loc.longitude as longitude,loc.altitude as altitude,study.title as study_title,collected_rel.eventDate as event_date,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel as source_specimen_life_stage,targetSpecimen.lifeStageLabel as target_specimen_life_stage,sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record,targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record,sourceSpecimen.bodyPartLabel as source_specimen_body_part,targetSpecimen.bodyPartLabel as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed as target_specimen_total_count,targetSpecimen.totalNumberConsumedPercent as target_specimen_total_count_percent,targetSpecimen.totalVolumeInMl as target_specimen_total_volume_ml,targetSpecimen.totalVolumePercent as target_specimen_total_volume_ml_percent,targetSpecimen.frequencyOfOccurrence as target_specimen_frequency_of_occurrence,targetSpecimen.frequencyOfOccurrencePercent as target_specimen_frequency_of_occurrence_percent,loc.footprintWKT as footprintWKT,loc.locality as locality";
    }

    private static String expectedInteractionClause(InteractType... interactions) {
        return "(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(interactions) + "]->(targetSpecimen:Specimen)";
    }

    private static String expectedMatchClause(String expectedInteractionClause, boolean hasSpatialConstraints, boolean requestedSpatialInfo) {
        return expectedMatchClause(expectedInteractionClause, hasSpatialConstraints, requestedSpatialInfo, false);
    }

    private static String expectedMatchClause(String expectedInteractionClause, boolean hasSpatialConstraints, boolean requestedSpatialInfo, boolean refutes) {
        String argumentType = refutes ? "REFUTES" : "COLLECTED";
        return expectedMatchClause(expectedInteractionClause, hasSpatialConstraints, requestedSpatialInfo, argumentType);
    }

    private static String expectedMatchClause(String expectedInteractionClause, boolean hasSpatialConstraints, boolean requestedSpatialInfo, String studyRel) {
        String spatialClause = " ";
        if (requestedSpatialInfo && hasSpatialConstraints) {
            spatialClause = ", (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) ";
        } else {
            spatialClause = " ";
        }

        return "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-" + expectedInteractionClause + "-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:" + studyRel + "]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset)" + spatialClause;
    }

    public static final String EXPECTED_RETURN_CLAUSE = "RETURN " +
            "sourceTaxon.externalId as source_taxon_external_id," +
            "sourceTaxon.name as source_taxon_name," +
            "sourceTaxon.path as source_taxon_path," +
            "sourceSpecimen.occurrenceId as source_specimen_occurrence_id," +
            "sourceSpecimen.institutionCode as source_specimen_institution_code," +
            "sourceSpecimen.collectionCode as source_specimen_collection_code," +
            "sourceSpecimen.catalogNumber as source_specimen_catalog_number," +
            "sourceSpecimen.lifeStageLabel as source_specimen_life_stage," +
            "sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record," +
            "interaction.label as interaction_type," +
            "targetTaxon.externalId as target_taxon_external_id," +
            "targetTaxon.name as target_taxon_name," +
            "targetTaxon.path as target_taxon_path," +
            "targetSpecimen.occurrenceId as target_specimen_occurrence_id," +
            "targetSpecimen.institutionCode as target_specimen_institution_code," +
            "targetSpecimen.collectionCode as target_specimen_collection_code," +
            "targetSpecimen.catalogNumber as target_specimen_catalog_number," +
            "targetSpecimen.lifeStageLabel as target_specimen_life_stage," +
            "targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record," +
            "loc.latitude as latitude," +
            "loc.longitude as longitude," +
            "study.title as study_title";

    public static final String EXPECTED_RETURN_CLAUSE_DISTINCT = "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
            "RETURN sourceTaxon.externalId as source_taxon_external_id," +
            "sourceTaxon.name as source_taxon_name," +
            "sourceTaxon.path as source_taxon_path," +
            "NULL as source_specimen_occurrence_id," +
            "NULL as source_specimen_institution_code," +
            "NULL as source_specimen_collection_code," +
            "NULL as source_specimen_catalog_number," +
            "NULL as source_specimen_life_stage," +
            "NULL as source_specimen_basis_of_record," +
            "iType as interaction_type," +
            "targetTaxon.externalId as target_taxon_external_id," +
            "targetTaxon.name as target_taxon_name," +
            "targetTaxon.path as target_taxon_path," +
            "NULL as target_specimen_occurrence_id," +
            "NULL as target_specimen_institution_code," +
            "NULL as target_specimen_collection_code," +
            "NULL as target_specimen_catalog_number," +
            "NULL as target_specimen_life_stage," +
            "NULL as target_specimen_basis_of_record," +
            "NULL as latitude," +
            "NULL as longitude," +
            "NULL as study_title";

    private CypherQuery query;

    @Before
    public void clearQuery() {
        query = null;
    }

    @After
    public void validateQuery() {
        if (query != null) {
            try {
                validate(query);
            } catch (Throwable e) {
                e.printStackTrace();
                fail("query failed to validate: [" + e.getMessage() + "]");
            }
        }
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocations() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Actinopterygii' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Actinopterygii OR Chordata, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsWithSingleQuote() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Rocky Mounatain juniper (Juniperus scopulorum Sarg.)- forming witches' brooms with juvensecent foliage on 2 trees.", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND " +
                "(sourceTaxon.externalIds CONTAINS '| ' + 'Rocky Mounatain juniper (Juniperus scopulorum Sarg.)- forming witches\\' brooms with juvensecent foliage on 2 trees.' + ' |' " +
                "OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Rocky Mounatain juniper (Juniperus scopulorum Sarg.)- forming witches' brooms with juvensecent foliage on 2 trees. OR Chordata, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionTypesForTaxon() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        query = CypherQueryBuilder.buildInteractionTypeQuery(params);

        String concatInteractionTypes = InteractUtil.allInteractionsCypherClause();
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[rel:" + concatInteractionTypes + "]->(otherTaxon:Taxon) " +
                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Actinopterygii' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                "RETURN distinct(type(rel)) as interaction_type"));
        assertThat(query.getParams().toString(), is(is("{taxon_name=Actinopterygii OR Chordata}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinct() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Actinopterygii' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                EXPECTED_RETURN_CLAUSE_DISTINCT));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Actinopterygii OR Chordata, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnly() {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name"});
        }};

        assertInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnly(fieldParams);
    }

    @Test
    public void findInteractionForTargetTaxonNameOnly() {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/1102
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{"target_taxon_name"});
            put("sourceTaxon", new String[]{"Donald duckus"});
        }};

        query = buildInteractionQuery(fieldParams, SINGLE_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                "CYPHER 5 " +
                        "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON|PARASITE_OF|HAS_HOST|HAS_RESERVOIR_HOST|INTERACTS_WITH|TROPHICALLY_INTERACTS_WITH|HOST_OF|RESERVOIR_HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|ACQUIRES_NUTRIENTS_FROM|PROVIDES_NUTRIENTS_FOR|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|CO_OCCURS_WITH|CO_ROOSTS_WITH|HAS_ROOST|ROOST_OF|COMMENSALIST_OF|MUTUALIST_OF|AGGRESSOR_OF|HAS_AGGRESSOR|ALLELOPATH_OF|HAS_ALLELOPATH|HEMIPARASITE_OF|ROOTPARASITE_OF|HAS_ECTOMYCORRHIZAL_HOST|ECTOMYCORRHIZAL_HOST_OF|HAS_ARBUSCULAR_MYCORRHIZAL_HOST|ARBUSCULAR_MYCORRHIZAL_HOST_OF|RELATED_TO]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                        "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                        "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Donald duckus' + ' |')) " +
                        "RETURN collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Donald duckus}")));
    }

    @Test
    public void findInteractionsManyFields() {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{
                    "source_taxon_name",
                    "source_taxon_path",
                    "source_taxon_path_ids",
                    "source_specimen_occurrence_id",
                    "source_specimen_institution_code",
                    "source_specimen_collection_code",
                    "source_specimen_catalog_number",
                    "source_specimen_life_stage_id",
                    "source_specimen_life_stage",
                    "source_specimen_physiological_state_id",
                    "source_specimen_physiological_state",
                    "source_specimen_body_part_id",
                    "source_specimen_body_part",
                    "source_specimen_sex_id",
                    "source_specimen_sex",
                    "source_specimen_basis_of_record",
                    "interaction_type",
                    "target_taxon_name",
                    "target_taxon_path",
                    "target_taxon_path_ids",
                    "target_specimen_occurrence_id",
                    "target_specimen_institution_code",
                    "target_specimen_collection_code",
                    "target_specimen_catalog_number",
                    "target_specimen_life_stage_id",
                    "target_specimen_life_stage",
                    "target_specimen_physiological_state_id",
                    "target_specimen_physiological_state",
                    "target_specimen_body_part_id",
                    "target_specimen_body_part",
                    "target_specimen_sex_id",
                    "target_specimen_sex",
                    "target_specimen_basis_of_record",
                    "latitude",
                    "longitude",
                    "event_date",
                    "study_citation",
                    "study_url",
                    "study_source_citation",
                    "study_source_archive_uri",
            });
        }};

        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Enhydra lutris"});
                put("refutes", new String[]{"false"});
                put("interactionType", new String[]{"eats"});
                put("includeObservations", new String[]{"true"});
                put("limit", new String[]{"4096"});
                put("offset", new String[]{"0"});

            }
        };
        params.putAll(fieldParams);


        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(),
                is(
                        "CYPHER 5 " +
                                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON|PARASITE_OF|ATE|ENDOPARASITE_OF|HYPERPARASITE_OF|ECTOPARASITE_OF|KLEPTOPARASITE_OF|PARASITOID_OF|ENDOPARASITOID_OF|ECTOPARASITOID_OF|FARMS]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), (sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Enhydra lutris' + ' |')) " +
                                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                                "RETURN " +
                                "sourceTaxon.name as source_taxon_name," +
                                "sourceTaxon.path as source_taxon_path," +
                                "sourceTaxon.pathIds as source_taxon_path_ids," +
                                "sourceSpecimen.occurrenceId as source_specimen_occurrence_id," +
                                "sourceSpecimen.institutionCode as source_specimen_institution_code," +
                                "sourceSpecimen.collectionCode as source_specimen_collection_code," +
                                "sourceSpecimen.catalogNumber as source_specimen_catalog_number," +
                                "sourceSpecimen.lifeStageId as source_specimen_life_stage_id," +
                                "sourceSpecimen.lifeStageLabel as source_specimen_life_stage," +
                                "sourceSpecimen.physiologicalStateId as source_specimen_physiological_state_id," +
                                "sourceSpecimen.physiologicalStateLabel as source_specimen_physiological_state," +
                                "sourceSpecimen.bodyPartId as source_specimen_body_part_id," +
                                "sourceSpecimen.bodyPartLabel as source_specimen_body_part," +
                                "null as source_specimen_sex_id," +
                                "sourceSpecimen.sexLabel as source_specimen_sex," +
                                "sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record," +
                                "interaction.label as interaction_type," +
                                "targetTaxon.name as target_taxon_name," +
                                "targetTaxon.path as target_taxon_path," +
                                "targetTaxon.pathIds as target_taxon_path_ids," +
                                "targetSpecimen.occurrenceId as target_specimen_occurrence_id," +
                                "targetSpecimen.institutionCode as target_specimen_institution_code," +
                                "targetSpecimen.collectionCode as target_specimen_collection_code," +
                                "targetSpecimen.catalogNumber as target_specimen_catalog_number," +
                                "targetSpecimen.lifeStageId as target_specimen_life_stage_id," +
                                "targetSpecimen.lifeStageLabel as target_specimen_life_stage," +
                                "targetSpecimen.physiologicalStateId as target_specimen_physiological_state_id," +
                                "targetSpecimen.physiologicalStateLabel as target_specimen_physiological_state," +
                                "targetSpecimen.bodyPartId as target_specimen_body_part_id," +
                                "targetSpecimen.bodyPartLabel as target_specimen_body_part," +
                                "null as target_specimen_sex_id," +
                                "targetSpecimen.sexLabel as target_specimen_sex," +
                                "targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record," +
                                "loc.latitude as latitude," +
                                "loc.longitude as longitude," +
                                "collected_rel.eventDate as event_date," +
                                "study.citation as study_citation," +
                                "study.externalId as study_url," +
                                "dataset.citation as study_source_citation," +
                                "dataset.archiveURI as study_source_archive_uri"
                ));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Enhydra lutris}")));
    }
    @Test
    public void findInteractionsSourceSexId() {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{
                    "source_specimen_sex_id"
            });
        }};

        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Enhydra lutris"});
                put("refutes", new String[]{"false"});
                put("interactionType", new String[]{"eats"});
                put("includeObservations", new String[]{"true"});
                put("limit", new String[]{"4096"});
                put("offset", new String[]{"0"});

            }
        };
        params.putAll(fieldParams);


        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(),
                is(
                        "CYPHER 5 " +
                                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON|PARASITE_OF|ATE|ENDOPARASITE_OF|HYPERPARASITE_OF|ECTOPARASITE_OF|KLEPTOPARASITE_OF|PARASITOID_OF|ENDOPARASITOID_OF|ECTOPARASITOID_OF|FARMS]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), (sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Enhydra lutris' + ' |')) " +
                                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                                "RETURN null as source_specimen_sex_id"
                ));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Enhydra lutris}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnlyUsingCommaFields() {
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


        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Actinopterygii' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Actinopterygii OR Chordata, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithTaxa() {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name"});
        }};

        assertFindInteractionsAccordingToWithTaxa(fieldParams);
    }

    @Test
    public void findInteractionsAccordingToWithTaxaWithCommaFields() {
        Map<String, String[]> fieldParams = new HashMap<String, String[]>() {{
            put("fields", new String[]{"source_taxon_name,target_taxon_name"});
        }};

        assertFindInteractionsAccordingToWithTaxa(fieldParams);
    }

    protected void assertFindInteractionsAccordingToWithTaxa(Map<String, String[]> fieldParams) {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("sourceTaxon", new String[]{"Arthropoda"});

            }
        };
        params.putAll(fieldParams);


        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTargetTaxon("Arthropoda") + "AND " + hasTaxon("Arthropoda", "source") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(),
                is(is("{accordingTo=foo, source_taxon_name=Arthropoda, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnly() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTargetTaxon("Arthropoda") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnly2() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://inaturalist.org/bla"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTargetTaxon("Arthropoda") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=http://inaturalist.org/bla, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsDOIAccordingToWithTargetTaxaOnly2() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"10.1234/4325", "doi:10.332/222", "https://doi.org/10.444/222"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTargetTaxon("Arthropoda") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is(
                "{accordingTo=10.1234/4325 10.332/222 10.444/222, target_taxon_name=Arthropoda}")));

    }

    @Test
    public void findInteractionsAccordingToWithSexAndObservations() {
        // see https://github.com/globalbioticinteractions/msb-para/issues/12
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://arctos.database.museum/guid/MSB:Mamm:79902"});
                put("includeObservations", new String[]{"true"});
                put("field", new String[]{"source_taxon_name", "source_specimen_sex"});
            }
        };

        query = buildInteractionQuery(params, QueryType.forParams(params));
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON|PARASITE_OF|HAS_HOST|HAS_RESERVOIR_HOST|INTERACTS_WITH|TROPHICALLY_INTERACTS_WITH|HOST_OF|RESERVOIR_HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|ACQUIRES_NUTRIENTS_FROM|PROVIDES_NUTRIENTS_FOR|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|ADJACENT_TO|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|CO_OCCURS_WITH|CO_ROOSTS_WITH|HAS_ROOST|ROOST_OF|COMMENSALIST_OF|MUTUALIST_OF|AGGRESSOR_OF|HAS_AGGRESSOR|ALLELOPATH_OF|HAS_ALLELOPATH|HEMIPARASITE_OF|ROOTPARASITE_OF|HAS_ECTOMYCORRHIZAL_HOST|ECTOMYCORRHIZAL_HOST_OF|HAS_ARBUSCULAR_MYCORRHIZAL_HOST|ARBUSCULAR_MYCORRHIZAL_HOST_OF|RELATED_TO]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), (sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) RETURN sourceTaxon.name as source_taxon_name,sourceSpecimen.sexLabel as source_specimen_sex"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=https://arctos.database.museum/guid/MSB:Mamm:79902}")));

    }

    @Test
    public void findInteractionsWithCollectionTimeUnixEpoch() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("includeObservations", new String[]{"true"});
                put("interactionType", new String[]{"pathogenOf"});
                put("field", new String[]{
                        "source_taxon_name",
                        "target_taxon_name",
                        "collection_time_in_unix_epoch"}
                        );
            }
        };

        query = buildInteractionQuery(params, QueryType.forParams(params));
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PATHOGEN_OF]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN " +
                "sourceTaxon.name as source_taxon_name" +
                ",targetTaxon.name as target_taxon_name" +
                ",collected_rel.eventDate as event_date"));
        assertThat(query.getParams().toString(), is(is("{}")));

    }

    @Test
    public void findInteractionsWithEventDate() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("includeObservations", new String[]{"true"});
                put("interactionType", new String[]{"pathogenOf"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "event_date"});
            }
        };

        query = buildInteractionQuery(params, QueryType.forParams(params));
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PATHOGEN_OF]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN " +
                "sourceTaxon.name as source_taxon_name" +
                ",targetTaxon.name as target_taxon_name" +
                ",collected_rel.eventDate as event_date"));
        assertThat(query.getParams().toString(), is(is("{}")));

    }

    @Test
    public void findInteractionsSingleTaxonWithCollectionTimeUnixEpoch() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("includeObservations", new String[]{"true"});
                put("interactionType", new String[]{"pathogenOf"});
                put("sourceTaxon", new String[]{"Ariopsis felis"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "collection_time_in_unix_epoch"});
            }
        };

        query = buildInteractionQuery(params, QueryType.forParams(params));
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PATHOGEN_OF]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Ariopsis felis' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN " +
                "sourceTaxon.name as source_taxon_name" +
                ",targetTaxon.name as target_taxon_name" +
                ",collected_rel.eventDate as event_date"));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Ariopsis felis}"));

    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnlyEmptySource() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://inaturalist.org/bla"});
                put("sourceTaxon", new String[]{""});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTargetTaxon("Arthropoda") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=http://inaturalist.org/bla, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaOnly() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:" + InteractUtil.interactionsCypherClause(RELATED_TO) + "]->(targetTaxon:Taxon) " +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaOnlyTaxonIdPrefix() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("interactionType", new String[]{"preyedUponBy"});
                put("taxonIdPrefix", new String[]{"somePrefix"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
            put("source_taxon_prefix", "somePrefix.*");
            put("target_taxon_prefix", "somePrefix.*");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsNameThatLooksLikeId() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"urn:catalog:AMNH:Mammals:M-39582"});
                put("targetTaxon", new String[]{"Paradyschiria lineata Kessel, 1925"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        Map<String, String> expected = new HashMap<String, String>() {{
            put("source_taxon_name", "urn:catalog:AMNH:Mammals:M-39582");
            put("target_taxon_name", "Paradyschiria lineata Kessel, 1925");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractions() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:" + InteractUtil.interactionsCypherClause(RELATED_TO) + "]->(targetTaxon:Taxon) " +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"
        ));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void accordingToDataset() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("accordingTo", new String[]{"globi:some/namespace"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions", "number_of_studies", "number_of_sources"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (study:Reference)-[:IN_DATASET]->(dataset:Dataset {namespace: $accordingTo}) " +
                "WITH study " +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + createInteractionTypeSelector(Collections.emptyList()) + "]" +
                "->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), (sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon, count(interaction) as interactionCount, count(distinct(id(study))) as studyCount, count(distinct(dataset.citation)) as sourceCount " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interactionCount as number_of_interactions,studyCount as number_of_studies,sourceCount as number_of_sources"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
            put("accordingTo", "some/namespace");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void accordingToDatasetWithBlanksInNamespace() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("accordingTo", new String[]{"globi:some/namespace    "});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions", "number_of_studies", "number_of_sources"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (study:Reference)-[:IN_DATASET]->(dataset:Dataset {namespace: $accordingTo}) " +
                "WITH study " +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + createInteractionTypeSelector(Collections.emptyList()) + "]" +
                "->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "WITH " +
                "distinct targetTaxon, interaction.label as iType, " +
                "sourceTaxon, count(interaction) as interactionCount, " +
                "count(distinct(id(study))) as studyCount, " +
                "count(distinct(dataset.citation)) as sourceCount " +
                "RETURN " +
                "sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name," +
                "interactionCount as number_of_interactions," +
                "studyCount as number_of_studies," +
                "sourceCount as number_of_sources"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
            put("accordingTo", "some/namespace");
        }};
        assertThat(query.getParams(), is(expected));
    }


    @Test
    public void findNumberOfStudiesForDistinctInteractionsAccordingTo() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("accordingTo", new String[]{"someSource"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions", "number_of_studies", "number_of_sources"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + createInteractionTypeSelector(Collections.emptyList()) + "]" +
                        "->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                        "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                        "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                        "WITH " +
                        "distinct targetTaxon, " +
                        "interaction.label as iType, " +
                        "sourceTaxon, " +
                        "count(interaction) as interactionCount, " +
                        "count(distinct(id(study))) as studyCount, " +
                        "count(distinct(dataset.citation)) as sourceCount " +
                        "RETURN " +
                        "sourceTaxon.name as source_taxon_name," +
                        "targetTaxon.name as target_taxon_name," +
                        "interactionCount as number_of_interactions," +
                        "studyCount as number_of_studies," +
                        "sourceCount as number_of_sources"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
            put("accordingTo", "someSource");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractionsExcludeChildTaxa() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("excludeChildTaxa", new String[]{"true"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:" + InteractUtil.interactionsCypherClause(RELATED_TO) + "]->(targetTaxon:Taxon) " +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['Arthropoda']) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractionsExactNameMatchOnly() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:" + InteractUtil.interactionsCypherClause(RELATED_TO) + "]->(targetTaxon:Taxon) " +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['Arthropoda']) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexTargetTaxaNumberOfInteractionsInteractionType() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "interaction_type"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:" + InteractUtil.interactionsCypherClause(RELATED_TO) + "]->(targetTaxon:Taxon) " +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.label as interaction_type"));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("target_taxon_name", "Arthropoda");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsTaxaInteractionIndexInteractionTypeTargetTaxaNumberOfInteractions() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Mammalia"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("interactionType", new String[]{"endoparasiteOf"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_interactions"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:ENDOPARASITE_OF]->(targetTaxon:Taxon) " +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Mammalia' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,interaction.count as number_of_interactions"
        ));
        Map<String, String> expected = new HashMap<String, String>() {{
            put("source_taxon_name", "Mammalia");
            put("target_taxon_name", "Arthropoda");
        }};
        assertThat(query.getParams(), is(expected));
    }

    @Test
    public void findInteractionsAccordingToWithTargetTaxaOnly3() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"http://inaturalist.org/bla", "http://inaturalist.org/bla2"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTargetTaxon("Arthropoda") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=http://inaturalist.org/bla http://inaturalist.org/bla2, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnly() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE " + hasTaxon("Arthropoda", "source") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo, source_taxon_name=Arthropoda}")));
    }

    @Test
    public void findRefutingAccordingTo() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo"});
                put("refutes", new String[]{"t"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT_REFUTING +
                        "WHERE " + hasTaxon("Arthropoda", "source") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo, source_taxon_name=Arthropoda}")));
    }

    @Test
    public void findRefutingAndNonRefutingAccordingTo() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo"});
                put("refutes", new String[]{"t", "f"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT_REFUTING_AND_SUPPORTING +
                        "WHERE " + hasTaxon("Arthropoda", "source") +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo, source_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnlyAndExactMatchOnly() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("accordingTo", new String[]{"foo"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE (sourceTaxon.name IS NOT NULL AND sourceTaxon.name IN ['Arthropoda']) " +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo, source_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnlyAndExcludeChildTaxa() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("excludeChildTaxa", new String[]{"true"});
                put("accordingTo", new String[]{"foo"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WHERE (sourceTaxon.name IS NOT NULL AND sourceTaxon.name IN ['Arthropoda']) " +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo, source_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxaOnlyAndExactMatchOnlyIncludeObservations() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Insecta"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['Insecta']) " +
                "AND (sourceTaxon.name IS NOT NULL AND sourceTaxon.name IN ['Arthropoda']) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Arthropoda, target_taxon_name=Insecta}")));
    }

    @Test
    public void prefixSelectorForName() {
        assertThat(CypherQueryBuilder.selectorPrefixForName("EOL:123", true), is(""));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla:123", true), is(""));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla:123", false), is(""));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla name", false), is(""));
        assertThat(CypherQueryBuilder.selectorPrefixForName("bla name", true), is(""));
    }

    @Test
    public void prefixSelectorForBOLDExternalIdName() {
        assertThat(
                CypherQueryBuilder.selectorPrefixForName("http://www.boldsystems.org/index.php/Public_BarcodeCluster?clusteruri=BOLD:AEI8875", true),
                is(""));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdOnlyAndExactMatchOnlyIncludeObservations2() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"EOL:123"});
                put("targetTaxon", new String[]{"Insecta"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['Insecta']) " +
                "AND (sourceTaxon.externalId IS NOT NULL AND sourceTaxon.externalId IN ['EOL:123']) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=EOL:123, target_taxon_name=Insecta}")));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdAndNamesOnlyAndExactMatchOnlyIncludeObservations2() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"EOL:123", "some name"});
                put("targetTaxon", new String[]{"Insecta"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['Insecta']) " +
                "AND (sourceTaxon.name IS NOT NULL AND sourceTaxon.name IN ['some name']) OR (sourceTaxon.externalId IS NOT NULL AND sourceTaxon.externalId IN ['EOL:123']) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=EOL:123 OR some name, target_taxon_name=Insecta}"));
    }

    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdTargetTaxonIdAndNameOnlyAndExactMatchOnlyIncludeObservations2() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"EOL:123", "some name"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['some name']) OR (targetTaxon.externalId IS NOT NULL AND targetTaxon.externalId IN ['EOL:123']) " +
                "AND (sourceTaxon.name IS NOT NULL AND sourceTaxon.name IN ['Arthropoda']) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{" +
                "source_taxon_name=Arthropoda," +
                " target_taxon_name=EOL:123 OR some name}")));
    }


    @Test
    public void findInteractionsAccordingToWithSourceTaxonIdTargetUnsupportedTaxonIdAndNameOnlyAndExactMatchOnlyIncludeObservations2() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("exactNameMatchOnly", new String[]{"true"});
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"FOO:123", "some name"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (targetTaxon.name IS NOT NULL AND targetTaxon.name IN ['FOO:123','some name']) " +
                "AND (sourceTaxon.name IS NOT NULL AND sourceTaxon.name IN ['Arthropoda']) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Arthropoda, target_taxon_name=FOO:123 OR some name}"));
    }

    @Test
    public void findInteractionsAccordingToNoTaxa() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo}")));
    }

    @Test
    public void findInteractionsAccordingToMultipleNoTaxa() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("accordingTo", new String[]{"foo", "gomexsi.edu"});
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        EXPECTED_MATCH_CLAUSE_DISTINCT +
                        "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=foo gomexsi.edu}")));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaLocationsDistinctTaxonNamesOnlyFlippedFields() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("field", new String[]{"target_taxon_name", "source_taxon_name", "source_taxon_path_ranks"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + '" + "Actinopterygii" + "' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                "WITH distinct targetTaxon, interaction.label as iType, sourceTaxon RETURN targetTaxon.name as target_taxon_name,sourceTaxon.name as source_taxon_name,sourceTaxon.pathNames as source_taxon_path_ranks"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Actinopterygii OR Chordata, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findInteractionCountOfEnhydraIncludeObservations() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Enhydra"});
                put("targetTaxon", new String[]{"Arthropoda"});
                put("field", new String[]{"target_taxon_name", "study_citation"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE " + hasTargetTaxon("Arthropoda") + "AND " + hasTaxon("Enhydra", "source") +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN targetTaxon.name as target_taxon_name,study.citation as study_citation"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=Enhydra, target_taxon_name=Arthropoda}")));
    }

    @Test
    public void findTaxaAtLocationsDistinct() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "WITH loc " +
                "MATCH (taxon:Taxon)<-[:CLASSIFIED_AS]-(specimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "RETURN distinct(taxon.name) as taxon_name, taxon.commonNames as taxon_common_names, taxon.externalId as taxon_external_id, taxon.path as taxon_path, taxon.pathIds as taxon_path_ids, taxon.pathNames as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctInteractionTypes() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "WITH loc " +
                "MATCH (taxon:Taxon)<-[:CLASSIFIED_AS]-(specimen:Specimen)-[:COLLECTED_AT]->(loc:Location), (taxon:Taxon)-[:" + InteractUtil.interactionsCypherClause(PREYS_UPON, PARASITE_OF) + "]->(otherTaxon:Taxon) " +
                "RETURN distinct(taxon.name) as taxon_name, taxon.commonNames as taxon_common_names, taxon.externalId as taxon_external_id, taxon.path as taxon_path, taxon.pathIds as taxon_path_ids, taxon.pathNames as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findWithDispersalInteractionType() {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Collections.singletonList("dispersalVectorOf"));
        assertThat(typeSelector, is("DISPERSAL_VECTOR_OF"));
    }

    @Test
    public void findWithDispersalInteractionType2() {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Collections.singletonList("hasDispersalVector"));
        assertThat(typeSelector, is("HAS_DISPERAL_VECTOR"));
    }

    @Test
    public void findWithDispersalInteractionTypeParasitoidByInternalName() {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Collections.singletonList("HAS_PARASITOID"));
        assertThat(typeSelector, is("HAS_PARASITOID|HAS_ENDOPARASITOID|HAS_ECTOPARASITOID"));
    }

    @Test
    public void findWithDispersalInteractionTypeParasitoidByIRI() {
        final String typeSelector = CypherQueryBuilder.createInteractionTypeSelector(Collections.singletonList("http://purl.obolibrary.org/obo/RO_0002632"));
        assertThat(typeSelector, is("ECTOPARASITE_OF"));
    }

    @Test
    public void findTaxaAtLocationsKillDistinctInteractionTypes() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("interactionType", new String[]{"kills", "parasiteOf"});
            }
        };

        query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "WITH loc " +
                "MATCH (taxon:Taxon)<-[:CLASSIFIED_AS]-(specimen:Specimen)-[:COLLECTED_AT]->(loc:Location), (taxon:Taxon)-[:" + InteractUtil.interactionsCypherClause(KILLS, PARASITE_OF) + "]->(otherTaxon:Taxon) " +
                "RETURN distinct(taxon.name) as taxon_name, taxon.commonNames as taxon_common_names, taxon.externalId as taxon_external_id, taxon.path as taxon_path, taxon.pathIds as taxon_path_ids, taxon.pathNames as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctInteractionTypesSpecificFields() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
                put("field", new String[]{ResultField.TAXON_NAME.getLabel()});
            }
        };

        query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "WITH loc " +
                "MATCH (taxon:Taxon)<-[:CLASSIFIED_AS]-(specimen:Specimen)-[:COLLECTED_AT]->(loc:Location), (taxon:Taxon)-[:" + InteractUtil.interactionsCypherClause(PREYS_UPON, PARASITE_OF) + "]->(otherTaxon:Taxon) " +
                "RETURN distinct(taxon.name) as taxon_name"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctNoSpatialParam() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
            }
        };

        query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (taxon:Taxon) " +
                "RETURN distinct(taxon.name) as taxon_name, taxon.commonNames as taxon_common_names, taxon.externalId as taxon_external_id, taxon.path as taxon_path, taxon.pathIds as taxon_path_ids, taxon.pathNames as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findTaxaAtLocationsDistinctNoSpatialInteractTypesParam() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        query = CypherQueryBuilder.createDistinctTaxaInLocationQuery(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (taxon:Taxon)-[:" + InteractUtil.interactionsCypherClause(PREYS_UPON, PARASITE_OF) + "]->(otherTaxon:Taxon) " +
                "RETURN distinct(taxon.name) as taxon_name, taxon.commonNames as taxon_common_names, taxon.externalId as taxon_external_id, taxon.path as taxon_path, taxon.pathIds as taxon_path_ids, taxon.pathNames as taxon_path_ranks"));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findInteractionForLocationOnly() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findInteractionForLocationOnlyDistinct() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_SPATIAL +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                EXPECTED_RETURN_CLAUSE_DISTINCT));
        assertThat(query.getParams().isEmpty(), is(true));
    }

    @Test
    public void findInteractionForSourceAndTargetTaxaNoLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
                put("targetTaxon", new String[]{"Arthropoda"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE " + hasTargetTaxon("Arthropoda") +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Actinopterygii' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Actinopterygii OR Chordata, target_taxon_name=Arthropoda}"));
    }

    @Test
    public void findInteractions() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{""});
                put("targetTaxon", new String[]{""});
                put("field", new String[]{"source_taxon_name", "interaction_type", "target_taxon_name"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:" + InteractUtil.interactionsCypherClause(RELATED_TO) + "]->(targetTaxon:Taxon) " +
                "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,targetTaxon.name as target_taxon_name";
        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyNoLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("targetTaxon", new String[]{"Arthropoda"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{target_taxon_name=Arthropoda}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyByInteractionType() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                expectedMatchClause(expectedInteractionClause(PREYS_UPON, PARASITE_OF), false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Arthropoda, target_taxon_name=Mammalia}"));
    }

    @Test
    public void findInteractionForTargetTaxaOnlyByInteractionTypeDistinct() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"preysOn", "parasiteOf"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                expectedMatchClause(expectedInteractionClause(PREYS_UPON, PARASITE_OF), false, false) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                EXPECTED_RETURN_CLAUSE_DISTINCT;
        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Arthropoda, target_taxon_name=Mammalia}"));
    }


    @Test
    public void findInteractionForTargetTaxaPollinates() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"pollinatedBy"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                expectedMatchClause(expectedInteractionClause(POLLINATED_BY), false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Arthropoda, target_taxon_name=Mammalia}"));
    }

    @Test
    public void findSymbioticInteractions() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"symbiontOf"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                expectedMatchClause(expectedInteractionClause(SYMBIONT_OF), false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Arthropoda, target_taxon_name=Mammalia}"));
    }

    @Test
    public void findAnyInteractions() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{"interactsWith"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                expectedMatchClause(expectedInteractionClause(INTERACTS_WITH),
                        false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Arthropoda, target_taxon_name=Mammalia}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInteractionForTargetTaxaOnlyByInteractionTypeNotSupported() {
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
    public void findInteractionForTargetTaxaOnlyByEmptyInteractionTypeNotSupported() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Arthropoda"});
                put("targetTaxon", new String[]{"Mammalia"});
                put("interactionType", new String[]{""});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true) +
                EXTERNAL_WHERE_CLAUSE_MAMMALIA +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Arthropoda' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Arthropoda, target_taxon_name=Mammalia}"));
    }

    @Test
    public void findInteractionForAnimaliaAndAnimalia() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Animalia"});
                put("targetTaxon", new String[]{"Animalia"});
                put("interactionType", new String[]{"interactsWith"});
                put("field", new String[]{"source_taxon_name", "source_taxon_external_id", "target_taxon_name", "target_taxon_external_id", "interaction_type", "number_of_interactions"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)-[interaction:PREYS_UPON|PARASITE_OF|HAS_HOST|HAS_RESERVOIR_HOST|INTERACTS_WITH|TROPHICALLY_INTERACTS_WITH|HOST_OF|RESERVOIR_HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|ACQUIRES_NUTRIENTS_FROM|PROVIDES_NUTRIENTS_FOR|HAS_VECTOR|VECTOR_OF|VISITED_BY|VISITS|FLOWERS_VISITED_BY|VISITS_FLOWERS_OF|INHABITED_BY|INHABITS|CREATES_HABITAT_FOR|HAS_HABITAT|LIVED_ON_BY|LIVES_ON|LIVED_INSIDE_OF_BY|LIVES_INSIDE_OF|LIVED_NEAR_BY|LIVES_NEAR|LIVED_UNDER_BY|LIVES_UNDER|LIVES_WITH|ENDOPARASITE_OF|HAS_ENDOPARASITE|HYPERPARASITE_OF|HAS_HYPERPARASITE|ECTOPARASITE_OF|HAS_ECTOPARASITE|KLEPTOPARASITE_OF|HAS_KLEPTOPARASITE|PARASITOID_OF|HAS_PARASITOID|ENDOPARASITOID_OF|HAS_ENDOPARASITOID|ECTOPARASITOID_OF|HAS_ECTOPARASITOID|GUEST_OF|HAS_GUEST_OF|FARMED_BY|FARMS|DAMAGED_BY|DAMAGES|DISPERSAL_VECTOR_OF|HAS_DISPERAL_VECTOR|KILLED_BY|KILLS|EPIPHITE_OF|HAS_EPIPHITE|LAYS_EGGS_ON|HAS_EGGS_LAYED_ON_BY|LAYS_EGGS_IN|HAS_EGGS_LAYED_IN_BY|HAS_ROOST|ROOST_OF|COMMENSALIST_OF|MUTUALIST_OF|ALLELOPATH_OF|HAS_ALLELOPATH|HEMIPARASITE_OF|ROOTPARASITE_OF|HAS_ECTOMYCORRHIZAL_HOST|ECTOMYCORRHIZAL_HOST_OF|HAS_ARBUSCULAR_MYCORRHIZAL_HOST|ARBUSCULAR_MYCORRHIZAL_HOST_OF]->(targetTaxon:Taxon) " +
                "WHERE (targetTaxon.externalIds IS NOT NULL AND (targetTaxon.externalIds CONTAINS '| ' + 'Animalia' + ' |')) " +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Animalia' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,sourceTaxon.externalId as source_taxon_external_id,targetTaxon.name as target_taxon_name,targetTaxon.externalId as target_taxon_external_id,interaction.label as interaction_type,interaction.count as number_of_interactions";
        query = buildInteractionQuery(params, MULTI_TAXON_DISTINCT_BY_NAME_ONLY);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Animalia, target_taxon_name=Animalia}"));
    }

    @Test
    public void findInteractionForSourceTaxaOnlyNoLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("sourceTaxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Actinopterygii' + ' |' OR sourceTaxon.externalIds CONTAINS '| ' + 'Chordata' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(params, MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{source_taxon_name=Actinopterygii OR Chordata}"));
    }

    @Test
    public void findInteractionNoParams() {
        String expectedQuery = EXPECTED_CYPHER_VERSION +
                EXPECTED_MATCH_CLAUSE_ALL +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                EXPECTED_RETURN_CLAUSE;
        query = buildInteractionQuery(new HashMap<String, String[]>(), MULTI_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(expectedQuery));
        assertThat(query.getParams().toString(), is("{}"));
    }

    @Test
    public void findPreysOnWithLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset), " +
                "(sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + HAS_TARGET_TAXON_PLANTAE +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                expectedReturnClause()));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Homo sapiens, target_taxon_name=Plantae}"));
    }

    @Test
    public void findPlantPreyObservationsWithoutLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE " +
                HAS_TARGET_TAXON_PLANTAE +
                "AND " +
                hasTaxon("Homo sapiens", "source") +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                expectedReturnClause()));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Homo sapiens, target_taxon_name=Plantae}"));
    }

    @Test
    public void findPlantParasiteObservationsWithoutLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(PARASITE_OF) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE " + HAS_TARGET_TAXON_PLANTAE +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " + expectedReturnClause()));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Homo sapiens, target_taxon_name=Plantae}"));
    }

    @Test
    public void findPreyObservationsNoLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        query = buildInteractionQuery("Homo sapiens", "preysOn", null, params, SINGLE_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " + expectedReturnClause()));
        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Homo sapiens}"));
    }

    @Test
    public void findKillsObservationsNoLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        query = buildInteractionQuery("Homo sapiens", "kills", null, params, SINGLE_TAXON_ALL);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(KILLS) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                "OPTIONAL MATCH (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " + expectedReturnClause()));

        assertThat(query.getParams().toString(),
                is("{source_taxon_name=Homo sapiens}"));
    }

    @Test
    public void findDistinctPreyWithLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_DISTINCT);

        Map<String, String> params1 = query.getParams();
        assertThat(params1.size(), is(2));
        assertThat(params1.get("source_taxon_name"), is("Homo sapiens"));
        assertThat(params1.get("target_taxon_name"), is("Plantae"));

        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset), " +
                "(sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "AND " + HAS_TARGET_TAXON_PLANTAE +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
    }

    @Test
    public void findDistinctPlantPreyWithoutLocation() {
        Map<String, String[]> params = new HashMap<>();
        query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_DISTINCT);

        Map<String, String> params1 = query.getParams();
        assertThat(params1.size(), is(2));
        assertThat(params1.get("source_taxon_name"), is("Homo sapiens"));
        assertThat(params1.get("target_taxon_name"), is("Plantae"));


        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH " +
                "(sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(PREYS_UPON) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE " + HAS_TARGET_TAXON_PLANTAE +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                "RETURN sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));

    }

    @Test
    public void findDistinctPlantParasiteWithoutLocation() {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_DISTINCT);

        Map<String, String> params1 = query.getParams();
        assertThat(params1.size(), is(2));
        assertThat(params1.get("source_taxon_name"), is("Homo sapiens"));
        assertThat(params1.get("target_taxon_name"), is("Plantae"));

        String expectedQuery = EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:" + InteractUtil.interactionsCypherClause(PARASITE_OF) + "]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) " +
                "WHERE " + HAS_TARGET_TAXON_PLANTAE +
                "AND (sourceTaxon.externalIds IS NOT NULL AND (sourceTaxon.externalIds CONTAINS '| ' + 'Homo sapiens' + ' |')) " +
                "RETURN " +
                "sourceTaxon.name as source_taxon_name,interaction.label as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name";

        assertThat(query.getVersionedQuery(), is(expectedQuery));
    }

    @Test
    public void statsWithBBox() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
            }
        };

        query = spatialInfo(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (loc:Location) WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "WITH loc " +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)<-[c:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset), (sourceSpecimen:Specimen)-[interact]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "WHERE interact.inverted IS NULL " +
                "RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(dataset)) as `number of distinct study sources`, count(c.eventDate) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{}"));
    }

    @Test
    public void statsWithBBoxAndSource() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-67.87,12.79,-57.08,23.32"});
                put("source", new String[]{"mySource"});
            }
        };

        query = spatialInfo(params);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (loc:Location) " +
                "WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) " +
                "WITH loc MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)<-[c:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset), (sourceSpecimen:Specimen)-[interact]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), (sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location) " +
                "WHERE interact.inverted IS NULL AND dataset.citation = $source " +
                "RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(dataset)) as `number of distinct study sources`, count(c.eventDate) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{source=mySource}"));
    }

    @Test
    public void stats() {
        query = spatialInfo(null);
        assertThat(query.getVersionedQuery(), is(EXPECTED_CYPHER_VERSION +
                "MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)<-[c:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset), " +
                "(sourceSpecimen:Specimen)-[interact]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon) " +
                "WHERE interact.inverted IS NULL " +
                "RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(dataset)) as `number of distinct study sources`, count(c.eventDate) as `number of interactions with timestamp`, NULL as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
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
                , is(" MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon), " +
                        "(sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset), " +
                        "(sourceSpecimen:Specimen)-[:COLLECTED_AT]->(loc:Location)" +
                        " WHERE loc.lnglat IS NOT NULL AND point.withinBBox(loc.lnglat, POINT({longitude: -67.87, latitude: 12.79}), POINT({longitude: -57.08, latitude: 23.32})) "));
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
                , is(" MATCH (sourceTaxon:Taxon)<-[:CLASSIFIED_AS]-(sourceSpecimen:Specimen)-[interaction:PREYS_UPON]->(targetSpecimen:Specimen)-[:CLASSIFIED_AS]->(targetTaxon:Taxon)" +
                        ", (sourceSpecimen:Specimen)<-[collected_rel:COLLECTED]-(study:Reference)-[:IN_DATASET]->(dataset:Dataset) "));
    }

    @Test
    public void locationsNoConstraints() {
        assertThat(locations().getVersionedQuery(), is(EXPECTED_CYPHER_VERSION + "START loc = node:locations('latitude:*') RETURN loc.latitude as latitude, loc.longitude as longitude, loc.footprintWKT as footprintWKT"));
    }

    @Test
    public void locationsAccordingTo() {
        Map<String, String> params = new HashMap<String, String>() {
            {
                put("accordingTo", "some source");
            }
        };

        query = locations(params);

        assertThat(query.getVersionedQuery(), is(
                EXPECTED_ACCORDING_TO_START_CLAUSE +
                        "MATCH (study:Reference)-[:COLLECTED]->(specimen:Specimen)-[:COLLECTED_AT]->(location:Location) WITH " +
                        "DISTINCT(location) as loc RETURN loc.latitude as latitude, loc.longitude as longitude, loc.footprintWKT as footprintWKT"));
        assertThat(query.getParams().toString(), is("{accordingTo=some source}"));
    }

    @Test
    public void regexAccordingToGoMexSI() {
        String regex = CypherQueryBuilder.matchReferenceOrDataset(Collections.singletonList("http://gomexsi.tamucc.edu"));
        assertThat(regex, is("http://gomexsi.tamucc.edu http://gomexsi.tamucc.edu/"));

        regex = CypherQueryBuilder.matchReferenceOrDataset(Arrays.<String>asList("http://gomexsi.tamucc.edu", "https://example.com"));
        assertThat(regex, is("http://gomexsi.tamucc.edu https://example.com http://gomexsi.tamucc.edu/"));

        regex = CypherQueryBuilder.matchReferenceOrDataset(Collections.singletonList("https://example.com"));
        assertThat(regex, is("https://example.com"));
    }

    @Test
    public void accordingToiNaturalistShortcut() {
        List<String> accordingTo = CypherQueryBuilder.collectAccordingTo(new TreeMap() {{
            put("accordingTo", "inaturalist");
        }});
        assertThat(accordingTo, hasItem("globi:globalbioticinteractions/inaturalist"));
    }

    @Test
    public void accordingToArctosHttpHttps() {
        // arctos puts out occurrenceId that start with [https], but still searches for [http] in their specimen pages
        // related to https://github.com/globalbioticinteractions/msb-para/issues/12
        // and https://github.com/orgs/ArctosDB/discussions/4679
        // and https://github.com/globalbioticinteractions/globalbioticinteractions/issues/791#issuecomment-1120094653
        List<String> accordingTo = CypherQueryBuilder.collectAccordingTo(new TreeMap() {{
            put("accordingTo", "http://arctos.database.museum/guid/MSB:Para:36168");
        }});
        assertThat(accordingTo, hasItem("https://arctos.database.museum/guid/MSB:Para:36168"));
    }

    @Test(expected = QueryExecutionException.class)
    public void queryValidation() {
        String malformed = "malformed";
        CypherQuery cypherQuery = new CypherQuery(malformed);
        validate(cypherQuery);
    }

    @Test
    public void parsePagedPropertyInScientificNotation() {
        long actualValue = CypherQueryBuilder.parsePagedQueryLongValue("bla", 10, "1e+5");
        assertThat(actualValue, is(100000L));
    }

    @Test
    public void parsePagedPropertyInScientificNotationMinus() {
        long actualValue = CypherQueryBuilder.parsePagedQueryLongValue("bla", 10, "1e-5");
        assertThat(actualValue, is(0L));
    }

    @Test
    public void parsePagedPropertyInScientificNotation2() {
        long actualValue = CypherQueryBuilder.parsePagedQueryLongValue("bla", 10, "1+E5");
        assertThat(actualValue, is(100000L));
    }

    @Test
    public void parsePagedPropertyInScientificNotation3() {
        long actualValue = CypherQueryBuilder.parsePagedQueryLongValue("bla", 10, "1-E5");
        assertThat(actualValue, is(0L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedPagedProperty() {
        try {
            CypherQueryBuilder.parsePagedQueryLongValue("bla", 10, "thisaintnonumber");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("malformed query value [bla] found: [thisaintnonumber]. Expected some positive integer value (e.g., 1, 2, 400, 1000)."));
            throw ex;
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePagedProperty() {
        try {
            CypherQueryBuilder.parsePagedQueryLongValue("bla", 10, "-1");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("malformed query value [bla] found: [-1]. Expected some positive integer value (e.g., 1, 2, 400, 1000)."));
            throw ex;
        }

    }

}
