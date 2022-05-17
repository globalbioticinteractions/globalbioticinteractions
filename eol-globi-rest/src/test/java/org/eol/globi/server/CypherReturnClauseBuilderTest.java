package org.eol.globi.server;

import org.junit.Ignore;
import org.junit.Test;

import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class CypherReturnClauseBuilderTest {

    @Test
    public void multiTaxonAllUnknownReturnFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                unknownFields());
        assertThat(query.toString(), is(" RETURN " +
                "sourceTaxon.externalId as source_taxon_external_id," +
                "sourceTaxon.name as source_taxon_name," +
                "sourceTaxon.path as source_taxon_path," +
                "sourceSpecimen.occurrenceId as source_specimen_occurrence_id," +
                "sourceSpecimen.institutionCode as source_specimen_institution_code," +
                "sourceSpecimen.collectionCode as source_specimen_collection_code," +
                "sourceSpecimen.catalogNumber as source_specimen_catalog_number,"+
                "sourceSpecimen.lifeStageLabel as source_specimen_life_stage," +
                "sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record," +
                "interaction.label as interaction_type," +
                "targetTaxon.externalId as target_taxon_external_id," +
                "targetTaxon.name as target_taxon_name," +
                "targetTaxon.path as target_taxon_path," +
                "targetSpecimen.occurrenceId as target_specimen_occurrence_id," +
                "targetSpecimen.institutionCode as target_specimen_institution_code," +
                "targetSpecimen.collectionCode as target_specimen_collection_code," +
                "targetSpecimen.catalogNumber as target_specimen_catalog_number,"+
                "targetSpecimen.lifeStageLabel as target_specimen_life_stage," +
                "targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record," +
                "loc.latitude as latitude," +
                "loc.longitude as longitude," +
                "study.title as study_title"));
    }

    @Test
    public void multiTaxonAllDatasetFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                new TreeMap<String, String[]>() {
                    {
                        put("field", new String[]{
                                "study_source_citation",
                                "study_source_archive_uri",
                                "study_source_last_seen_at"
                        });
                    }
                });
        assertThat(query.toString(), is(" RETURN " +
                "dataset.citation as study_source_citation," +
                "dataset.archiveURI as study_source_archive_uri," +
                "dataset.lastSeenAt as study_source_last_seen_at"));
    }

    @Test
    public void datasetNamespace() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                new TreeMap<String, String[]>() {
                    {
                        put("field", new String[]{
                                "study_source_citation",
                                "study_source_archive_uri",
                                "study_source_last_seen_at",
                                "study_source_id"
                        });
                    }
                });
        assertThat(query.toString(), is(" RETURN " +
                "dataset.citation as study_source_citation," +
                "dataset.archiveURI as study_source_archive_uri," +
                "dataset.lastSeenAt as study_source_last_seen_at," +
                "dataset.namespace as study_source_id"));
    }

    @Test
    public void multiTaxonAllUnknownReturnFieldsWithTaxonPrefix() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                taxonomyOnly());
        assertThat(query.toString(), is(" WITH " +
                "sourceTaxon, " +
                "sourceSpecimen, " +
                "interaction, " +
                "targetTaxon, " +
                "targetSpecimen, " +
                "loc, " +
                "study " +
                "MATCH sourceTaxon-[:SAME_AS*0..1]->sourceTaxonSameAs, targetTaxon-[:SAME_AS*0..1]->targetTaxonSameAs " +
                "WHERE " +
                "sourceTaxonSameAs.externalId =~ {source_taxon_prefix} AND " +
                "targetTaxonSameAs.externalId =~ {target_taxon_prefix} " +
                "WITH sourceTaxonSameAs as sourceTaxon, " +
                "sourceSpecimen, " +
                "interaction, " +
                "targetTaxonSameAs as targetTaxon, " +
                "targetSpecimen, " +
                "loc, " +
                "study " +
                "RETURN " +
                "sourceTaxon.externalId as source_taxon_external_id," +
                "sourceTaxon.name as source_taxon_name," +
                "sourceTaxon.path as source_taxon_path," +
                "sourceSpecimen.occurrenceId as source_specimen_occurrence_id," +
                "sourceSpecimen.institutionCode as source_specimen_institution_code," +
                "sourceSpecimen.collectionCode as source_specimen_collection_code," +
                "sourceSpecimen.catalogNumber as source_specimen_catalog_number,"+
                "sourceSpecimen.lifeStageLabel as source_specimen_life_stage," +
                "sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record," +
                "interaction.label as interaction_type," +
                "targetTaxon.externalId as target_taxon_external_id," +
                "targetTaxon.name as target_taxon_name," +
                "targetTaxon.path as target_taxon_path," +
                "targetSpecimen.occurrenceId as target_specimen_occurrence_id," +
                "targetSpecimen.institutionCode as target_specimen_institution_code," +
                "targetSpecimen.collectionCode as target_specimen_collection_code," +
                "targetSpecimen.catalogNumber as target_specimen_catalog_number,"+
                "targetSpecimen.lifeStageLabel as target_specimen_life_stage," +
                "targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record," +
                "loc.latitude as latitude," +
                "loc.longitude as longitude," +
                "study.title as study_title"));
    }

    @Test
    public void multiTaxonAllAllFieldsWithTaxonPrefix() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                knownFieldsWithTaxonomy());
        assertThat(query.toString(), is(" WITH " +
                "sourceTaxon, sourceSpecimen, interaction, targetTaxon, targetSpecimen, loc, study " +
                "MATCH sourceTaxon-[:SAME_AS*0..1]->sourceTaxonSameAs, targetTaxon-[:SAME_AS*0..1]->targetTaxonSameAs " +
                "WHERE " +
                "sourceTaxonSameAs.externalId =~ {source_taxon_prefix} AND " +
                "targetTaxonSameAs.externalId =~ {target_taxon_prefix} " +
                "WITH sourceTaxonSameAs as sourceTaxon, sourceSpecimen, interaction, targetTaxonSameAs as targetTaxon, targetSpecimen, loc, study " +
                "RETURN " +
                "sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name"));
    }

    @Test
    public void multiTaxonAllKnownReturnFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                knownFields());
        assertThat(query.toString(), is(" RETURN " +
                "sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name"));
    }

    @Ignore("see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/330")
    @Test
    public void multiTaxonAllWithNumberOfStudies() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT,
                new TreeMap<String, String[]>() {
                    {
                        put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_studies"});
                        put("accordingTo", new String[]{"someSource"});
                    }
                });
        assertThat(query.toString(), is(" WITH distinct targetTaxon, interaction.label as iType, sourceTaxon, count(distinct(id(study))) as studyCount " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,studyCount as number_of_studies " +
                "ORDER BY number_of_studies DESC"));
    }

    @Ignore("see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/330")
    @Test
    public void multiTaxonAllWithNumberOfStudiesIncludeObservations() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                new TreeMap<String, String[]>() {
                    {
                        put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_studies"});
                        put("accordingTo", new String[]{"someSource"});
                    }
                });
        assertThat(query.toString(), is(" RETURN " +
                "sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name," +
                "1 as number_of_studies " +
                "ORDER BY number_of_studies DESC"));
    }

    @Ignore("see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/330")
    @Test
    public void multiTaxonAllWithNumberOfStudiesWithIdPrefix() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT,
                new TreeMap<String, String[]>() {
                    {
                        put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_studies"});
                        put("accordingTo", new String[]{"someSource"});
                        put("taxonIdPrefix", new String[]{"somePrefix"});
                    }
                });
        assertThat(query.toString(), is(" WITH distinct targetTaxon, interaction.label as iType, sourceTaxon, count(distinct(id(study))) as studyCount " +
                "WITH sourceTaxon, iType, targetTaxon, studyCount " +
                "MATCH sourceTaxon-[:SAME_AS*0..1]->sourceTaxonSameAs, targetTaxon-[:SAME_AS*0..1]->targetTaxonSameAs " +
                "WHERE sourceTaxonSameAs.externalId =~ {source_taxon_prefix} AND targetTaxonSameAs.externalId =~ {target_taxon_prefix} " +
                "WITH sourceTaxonSameAs as sourceTaxon, iType, targetTaxonSameAs as targetTaxon, studyCount " +
                "RETURN sourceTaxon.name as source_taxon_name,targetTaxon.name as target_taxon_name,studyCount as number_of_studies " +
                "ORDER BY number_of_studies DESC"));
    }

    private TreeMap<String, String[]> knownFieldsWithTaxonomy() {
        return new TreeMap<String, String[]>(knownFields()) {
            {
                put("taxonIdPrefix", new String[]{"somePrefix"});
            }
        };
    }

    private TreeMap<String, String[]> taxonomyOnly() {
        return new TreeMap<String, String[]>() {
            {
                put("taxonIdPrefix", new String[]{"somePrefix"});
            }
        };
    }


    @Test
    public void multiTaxonDistinctKnownReturnFieldsWithTaxonomy() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT,
                knownFieldsWithTaxonomy());
        assertThat(query.toString(), is(
                " WITH distinct targetTaxon, " +
                        "interaction.label as iType, " +
                        "sourceTaxon " +
                        "WITH " +
                        "sourceTaxon, iType, targetTaxon " +
                        "MATCH sourceTaxon-[:SAME_AS*0..1]->sourceTaxonSameAs, targetTaxon-[:SAME_AS*0..1]->targetTaxonSameAs " +
                        "WHERE " +
                        "sourceTaxonSameAs.externalId =~ {source_taxon_prefix} AND " +
                        "targetTaxonSameAs.externalId =~ {target_taxon_prefix} " +
                        "WITH sourceTaxonSameAs as sourceTaxon, iType, targetTaxonSameAs as targetTaxon " +
                        "RETURN sourceTaxon.name as source_taxon_name," +
                        "targetTaxon.name as target_taxon_name"));
    }

    @Test
    public void multiTaxonDistinctKnownReturnFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT,
                knownFields());
        assertThat(query.toString(), is(" WITH distinct targetTaxon, " +
                "interaction.label as iType, " +
                "sourceTaxon " +
                "RETURN sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name"));
    }

    @Test
    public void multiTaxonDistinctByNameOnly() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT_BY_NAME_ONLY,
                knownFields());
        assertThat(query.toString(), is(" RETURN " +
                "sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name"));
    }

    @Test
    public void multiTaxonDistinctByNameOnlyDuplicates() {
        StringBuilder query = new StringBuilder();
        TreeMap<String, String[]> parameterMap = new TreeMap<String, String[]>() {
            {
                put("field", new String[]{
                        "source_taxon_name",
                        "target_taxon_name",
                        "source_taxon_name"});
            }
        };
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT_BY_NAME_ONLY,
                parameterMap);
        assertThat(query.toString(), is(" RETURN " +
                "sourceTaxon.name as source_taxon_name," +
                "targetTaxon.name as target_taxon_name"));
    }

    @Test
    public void multiTaxonSexLabelsIds() {
        StringBuilder query = new StringBuilder();
        TreeMap<String, String[]> parameterMap = new TreeMap<String, String[]>() {
            {
                put("field", new String[]{
                        "source_taxon_name",
                        "source_specimen_sex",
                        "source_specimen_sex_id",
                        "target_specimen_sex",
                        "target_specimen_sex_id",
                        "target_taxon_name"});
            }
        };
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                parameterMap);
        assertThat(query.toString(), is(" RETURN sourceTaxon.name as source_taxon_name,sourceSpecimen.sexLabel as source_specimen_sex,sourceSpecimen.sexId as source_specimen_sex_id,targetSpecimen.sexLabel as target_specimen_sex,targetSpecimen.sexId as target_specimen_sex_id,targetTaxon.name as target_taxon_name"));
    }

    @Test
    public void multiTaxonDistinctUnknownReturnFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT,
                unknownFields());
        assertThat(query.toString(), is(" WITH distinct targetTaxon, " +
                "interaction.label as iType, " +
                "sourceTaxon " +
                "RETURN sourceTaxon.externalId as source_taxon_external_id," +
                "sourceTaxon.name as source_taxon_name," +
                "sourceTaxon.path as source_taxon_path," +
                "NULL as source_specimen_occurrence_id," +
                "NULL as source_specimen_institution_code," +
                "NULL as source_specimen_collection_code," +
                "NULL as source_specimen_catalog_number,"+
                "NULL as source_specimen_life_stage," +
                "NULL as source_specimen_basis_of_record," +
                "iType as interaction_type," +
                "targetTaxon.externalId as target_taxon_external_id," +
                "targetTaxon.name as target_taxon_name," +
                "targetTaxon.path as target_taxon_path," +
                "NULL as target_specimen_occurrence_id," +
                "NULL as target_specimen_institution_code," +
                "NULL as target_specimen_collection_code," +
                "NULL as target_specimen_catalog_number,"+
                "NULL as target_specimen_life_stage," +
                "NULL as target_specimen_basis_of_record," +
                "NULL as latitude," +
                "NULL as longitude," +
                "NULL as study_title"));
    }

    private TreeMap<String, String[]> knownFields() {
        return new TreeMap<String, String[]>() {
            {
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };
    }

    @Test
    public void manyFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                new TreeMap<String, String[]>() {
                    {
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
                                "study_source_archive_uri"
                        });
                    }
                });
        assertThat(query.toString(), is(" RETURN sourceTaxon.name as source_taxon_name,sourceTaxon.path as source_taxon_path,sourceTaxon.pathIds as source_taxon_path_ids,sourceSpecimen.occurrenceId as source_specimen_occurrence_id,sourceSpecimen.institutionCode as source_specimen_institution_code,sourceSpecimen.collectionCode as source_specimen_collection_code,sourceSpecimen.catalogNumber as source_specimen_catalog_number,sourceSpecimen.lifeStageId as source_specimen_life_stage_id,sourceSpecimen.lifeStageLabel as source_specimen_life_stage,sourceSpecimen.physiologicalStateId as source_specimen_physiological_state_id,sourceSpecimen.physiologicalStateLabel as source_specimen_physiological_state,sourceSpecimen.bodyPartId as source_specimen_body_part_id,sourceSpecimen.bodyPartLabel as source_specimen_body_part,sourceSpecimen.sexId as source_specimen_sex_id,sourceSpecimen.sexLabel as source_specimen_sex,sourceSpecimen.basisOfRecordLabel as source_specimen_basis_of_record,interaction.label as interaction_type,targetTaxon.name as target_taxon_name,targetTaxon.path as target_taxon_path,targetTaxon.pathIds as target_taxon_path_ids,targetSpecimen.occurrenceId as target_specimen_occurrence_id,targetSpecimen.institutionCode as target_specimen_institution_code,targetSpecimen.collectionCode as target_specimen_collection_code,targetSpecimen.catalogNumber as target_specimen_catalog_number,targetSpecimen.lifeStageId as target_specimen_life_stage_id,targetSpecimen.lifeStageLabel as target_specimen_life_stage,targetSpecimen.physiologicalStateId as target_specimen_physiological_state_id,targetSpecimen.physiologicalStateLabel as target_specimen_physiological_state,targetSpecimen.bodyPartId as target_specimen_body_part_id,targetSpecimen.bodyPartLabel as target_specimen_body_part,targetSpecimen.sexId as target_specimen_sex_id,targetSpecimen.sexLabel as target_specimen_sex,targetSpecimen.basisOfRecordLabel as target_specimen_basis_of_record,loc.latitude as latitude,loc.longitude as longitude,collected_rel.eventDate as event_date,study.citation as study_citation,study.externalId as study_url,dataset.citation as study_source_citation,dataset.archiveURI as study_source_archive_uri"));
    }



    private TreeMap<String, String[]> unknownFields() {
        return new TreeMap<String, String[]>() {
            {
                put("field", new String[]{"foo", "bar"});
            }
        };
    }

}