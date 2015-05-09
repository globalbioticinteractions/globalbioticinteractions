package org.eol.globi.server;

import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.InteractUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.server.CypherQueryBuilder.QueryType.MULTI_TAXON_ALL;
import static org.eol.globi.server.CypherQueryBuilder.QueryType.MULTI_TAXON_DISTINCT;
import static org.eol.globi.server.CypherQueryBuilder.QueryType.SINGLE_TAXON_ALL;
import static org.eol.globi.server.CypherQueryBuilder.QueryType.SINGLE_TAXON_DISTINCT;
import static org.eol.globi.server.CypherQueryBuilder.appendMatchAndWhereClause;
import static org.eol.globi.server.CypherQueryBuilder.buildInteractionQuery;
import static org.eol.globi.server.CypherQueryBuilder.spatialInfo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CypherQueryBuilderTest {

    public static final String EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS = expectedInteractionClause(InteractUtil.allInteractionsCypherClause());
    public static final String EXPECTED_MATCH_CLAUSE_ALL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true);
    public static final String EXPECTED_MATCH_CLAUSE_DISTINCT = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, false);
    public static final String EXPECTED_MATCH_CLAUSE_SPATIAL = expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, true, true);
    public static final String RETURN_PREYS_ON_CLAUSE = expectedReturnClause("preysOn");
    public static final String RETURN_PARASITE_OF_CLAUSE = expectedReturnClause("parasiteOf");

    private static String expectedReturnClause(String interactionType) {
        return "RETURN sourceTaxon.name as source_taxon_name,'" + interactionType + "' as interaction_type,targetTaxon.name as target_taxon_name,loc.latitude? as latitude,loc.longitude? as longitude,loc.altitude? as altitude,study.title as study_title,collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch,ID(sourceSpecimen) as tmp_and_unique_source_specimen_id,ID(targetSpecimen) as tmp_and_unique_target_specimen_id,sourceSpecimen.lifeStageLabel? as source_specimen_life_stage,targetSpecimen.lifeStageLabel? as target_specimen_life_stage,sourceSpecimen.basisOfRecordLabel? as source_specimen_basis_of_record,targetSpecimen.basisOfRecordLabel? as target_specimen_basis_of_record,sourceSpecimen.bodyPartLabel? as source_specimen_body_part,targetSpecimen.bodyPartLabel? as target_specimen_body_part,sourceSpecimen.physiologicalStateLabel? as source_specimen_physiological_state,targetSpecimen.physiologicalStateLabel? as target_specimen_physiological_state,targetSpecimen.totalNumberConsumed? as target_specimen_total_count,targetSpecimen.totalVolumeInMl? as target_specimen_total_volume_ml,targetSpecimen.frequencyOfOccurrence? as target_specimen_frequency_of_occurrence";
    }

    private static String expectedInteractionClause(String interactions) {
        return "sourceSpecimen-[interaction:" + interactions + "]->targetSpecimen";
    }

    private static String expectedMatchClause(String expectedInteractionClause, boolean hasSpatialConstraints, boolean requestedSpatialInfo) {
        String spatialClause = requestedSpatialInfo ? (", sourceSpecimen-[" + (hasSpatialConstraints ? "" : "?") + ":COLLECTED_AT]->loc ") : " ";
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-" + expectedInteractionClause + "-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study" + spatialClause;
    }

    private static String expectedReplacementString(String interactionParamName) {
        String suffix = ",'POLLINATES','pollinates'),'POLLINATED_BY','pollinatedBy'),'PREYS_UPON','preysOn'),'PREYED_UPON_BY','preyedUponBy'),'PARASITE_OF','parasiteOf'),'HAS_PARASITE','hasParasite'),'VECTOR_OF','vectorOf'),'HAS_VECTOR','hasVector'),'PATHOGEN_OF','pathogenOf'),'HAS_PATHOGEN','hasPathogen'),'INTERACTS_WITH','interactsWith'),'SYMBIONT_OF','symbiontOf'),'HOST_OF','hostOf'),'HAS_HOST','hasHost'),'EATEN_BY','eatenBy'),'PREYED_UPON_BY','eatenBy'),'ATE','eats'),'PREYS_UPON','eats')";
        String prefix = "replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(";
        return prefix + interactionParamName + suffix;
    }

    public static final String EXPECTED_RETURN_CLAUSE = "RETURN sourceTaxon.externalId? as source_taxon_external_id," +
            "sourceTaxon.name as source_taxon_name," +
            "sourceTaxon.path? as source_taxon_path," +
            "sourceSpecimen.lifeStage? as source_specimen_life_stage," +
            "sourceSpecimen.basisOfRecordLabel? as source_specimen_basis_of_record," +
            expectedReplacementString("type(interaction)") + " as interaction_type," +
            "targetTaxon.externalId? as target_taxon_external_id," +
            "targetTaxon.name as target_taxon_name," +
            "targetTaxon.path? as target_taxon_path," +
            "targetSpecimen.lifeStage? as target_specimen_life_stage," +
            "targetSpecimen.basisOfRecordLabel? as target_specimen_basis_of_record," +
            "loc.latitude? as latitude," +
            "loc.longitude? as longitude," +
            "study.title as study_title";

    public static final String EXPECTED_RETURN_CLAUSE_DISTINCT = "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon " +
            "RETURN sTaxon.externalId? as source_taxon_external_id," +
            "sTaxon.name as source_taxon_name," +
            "sTaxon.path? as source_taxon_path," +
            "NULL as source_specimen_life_stage," +
            "NULL as source_specimen_basis_of_record," +
            expectedReplacementString("iType") + " as interaction_type," +
            "tTaxon.externalId? as target_taxon_external_id," +
            "tTaxon.name as target_taxon_name," +
            "tTaxon.path? as target_taxon_path," +
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
                EXPECTED_RETURN_CLAUSE));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=path:\\\"Actinopterygii\\\" OR path:\\\"Chordata\\\", target_taxon_name=path:\\\"Arthropoda\\\"}")));
    }

    @Test
    public void interactionReturnTerms() {
        assertThat(CypherQueryBuilder.appendInteractionTypeReturn(new StringBuilder(), "type(interaction)").toString(), is(expectedReplacementString("type(interaction)")));
    }

    @Test
    public void findInteractionTypesForTaxon() {
        HashMap<String, String[]> params = new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Actinopterygii", "Chordata"});
            }
        };

        CypherQuery query = CypherQueryBuilder.buildInteractionTypeQuery(params);
        assertThat(query.getQuery(), is("START taxon = node:taxonPaths({taxon_name}) MATCH taxon-[rel:PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|HOST_OF|POLLINATES|PERCHING_ON|ATE|SYMBIONT_OF|PREYED_UPON_BY|POLLINATED_BY|EATEN_BY|HAS_PARASITE|PERCHED_ON_BY|HAS_PATHOGEN|PATHOGEN_OF|HAS_VECTOR|VECTOR_OF]->otherTaxon RETURN distinct(type(rel)) as interaction_type"));
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
        assertThat(query.getQuery(), is("START study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo}) WITH study " +
                EXPECTED_MATCH_CLAUSE_DISTINCT +
                "WHERE (has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' AND has(sourceTaxon.path) AND sourceTaxon.path =~ '(.*(Arthropoda).*)' ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE (has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE (has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{accordingTo=(\\\\Qhttp://inaturalist.org/bla\\\\E), target_taxon_name=path:\\\"Arthropoda\\\"}")));
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
                "WHERE (has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE (has(sourceTaxon.path) AND sourceTaxon.path =~ '(.*(Arthropoda).*)' ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE (has(sourceTaxon.name) AND sourceTaxon.name IN ['Arthropoda'] ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE (has(sourceTaxon.name) AND sourceTaxon.name IN ['Arthropoda'] ) " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE has(targetTaxon.name) AND targetTaxon.name IN ['Insecta']" +
                " RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name"));
        assertThat(query.getParams().toString(), is(is("{source_taxon_name=name:\\\"Arthropoda\\\", target_taxon_name=name:\\\"Insecta\\\"}")));
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
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN sTaxon.name as source_taxon_name,tTaxon.name as target_taxon_name"));
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
                "WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon RETURN tTaxon.name as target_taxon_name,sTaxon.name as source_taxon_name,sTaxon.pathNames? as source_taxon_path_ranks"));
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
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Arthropoda).*)' " +
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
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
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc, taxon-[:PREYS_UPON|PARASITE_OF]->otherTaxon RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
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
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc, taxon-[:PREYS_UPON|PARASITE_OF]->otherTaxon RETURN distinct(taxon.name?) as taxon_name"));
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
        assertThat(query.getQuery(), is("START taxon = node:taxons('*:*') MATCH taxon-[:PREYS_UPON|PARASITE_OF]->otherTaxon RETURN distinct(taxon.name?) as taxon_name, taxon.commonNames? as taxon_common_names, taxon.externalId? as taxon_external_id, taxon.path? as taxon_path, taxon.pathIds? as taxon_path_ids, taxon.pathNames? as taxon_path_ranks"));
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
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
                "WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 " +
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
                expectedMatchClause(expectedInteractionClause("PREYS_UPON|PARASITE_OF"), false, true) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
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
                expectedMatchClause(expectedInteractionClause("PREYS_UPON|PARASITE_OF"), false, false) +
                "WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Mammalia).*)' " +
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
                expectedMatchClause(expectedInteractionClause("POLLINATED_BY"), false, true) +
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
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true) +
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
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true) +
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
                expectedMatchClause(EXPECTED_INTERACTION_CLAUSE_ALL_INTERACTIONS, false, true) +
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
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' " + RETURN_PREYS_ON_CLAUSE));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPlantPreyObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' " + RETURN_PREYS_ON_CLAUSE));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPlantParasiteObservationsWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PARASITE_OF]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' " + RETURN_PARASITE_OF_CLAUSE));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findPreyObservationsNoLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", null, params, SINGLE_TAXON_ALL);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc " + RETURN_PREYS_ON_CLAUSE));
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
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[:COLLECTED_AT]->loc WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 AND has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantPreyWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "preysOn", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name,'preysOn' as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
        assertThat(query.getParams().toString(), is("{source_taxon_name=path:\\\"Homo sapiens\\\", target_taxon_name=path:\\\"Plantae\\\"}"));
    }

    @Test
    public void findDistinctPlantParasiteWithoutLocation() throws IOException {
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        CypherQuery query = buildInteractionQuery("Homo sapiens", "parasiteOf", "Plantae", params, SINGLE_TAXON_DISTINCT);
        assertThat(query.getQuery(), is("START sourceTaxon = node:taxonPaths({source_taxon_name}) MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PARASITE_OF]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study WHERE has(targetTaxon.path) AND targetTaxon.path =~ '(.*(Plantae).*)' RETURN sourceTaxon.name as source_taxon_name,'parasiteOf' as interaction_type,collect(distinct(targetTaxon.name)) as target_taxon_name"));
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
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE not(has(interact.inverted)) RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
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
        assertThat(query.getQuery(), is("START loc = node:locations('latitude:*') WHERE loc.latitude < 23.32 AND loc.longitude > -67.87 AND loc.latitude > 12.79 AND loc.longitude < -57.08 WITH loc MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:COLLECTED_AT]->loc WHERE not(has(interact.inverted)) AND study.source = {source} RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, count(distinct(loc)) as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
        assertThat(query.getParams().toString(), is("{source=mySource}"));
    }

    @Test
    public void stats() throws IOException {
        CypherQuery query = spatialInfo(null);
        assertThat(query.getQuery(), is("START study = node:studies('*:*') MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study, sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon WHERE not(has(interact.inverted)) RETURN count(distinct(study)) as `number of distinct studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`, count(c.dateInUnixEpoch?) as `number of interactions with timestamp`, NULL as `number of distinct locations`, count(distinct(sourceTaxon.name + type(interact) + targetTaxon.name)) as `number of distinct interactions`"));
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
        }}, params, new StringBuilder(), CypherQueryBuilder.QueryType.MULTI_TAXON_ALL).toString();
        assertLocationMatchAndWhereClause(clause);
    }

    protected void assertLocationMatchAndWhereClause(String clause) {
        assertThat(clause.replaceAll("\\s+", " ")
                , is(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon" +
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
        }}, params, new StringBuilder(), CypherQueryBuilder.QueryType.MULTI_TAXON_ALL).toString();
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
        }}, params, new StringBuilder(), CypherQueryBuilder.QueryType.MULTI_TAXON_ALL).toString()
                , is(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:PREYS_UPON]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon" +
                ", sourceSpecimen<-[collected_rel:COLLECTED]-study, sourceSpecimen-[?:COLLECTED_AT]->loc "));
    }

}
