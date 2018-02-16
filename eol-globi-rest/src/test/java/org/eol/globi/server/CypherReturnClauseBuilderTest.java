package org.eol.globi.server;

import org.junit.Ignore;
import org.junit.Test;

import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CypherReturnClauseBuilderTest {

    @Test
    public void multiTaxonAllUnknownReturnFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_ALL,
                unknownFields());
        assertThat(query.toString(), is(" RETURN " +
                "sourceTaxon.externalId? as source_taxon_external_id," +
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
                "study.title as study_title"));
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
                "sourceTaxon.externalId? as source_taxon_external_id," +
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

    @Ignore("see https://github.com/jhpoelen/eol-globi-data/issues/330")
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

    @Ignore("see https://github.com/jhpoelen/eol-globi-data/issues/330")
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

    @Ignore("see https://github.com/jhpoelen/eol-globi-data/issues/330")
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
    public void multiTaxonDistinctUnknownReturnFields() {
        StringBuilder query = new StringBuilder();
        CypherReturnClauseBuilder.appendReturnClauseMap(
                query,
                QueryType.MULTI_TAXON_DISTINCT,
                unknownFields());
        assertThat(query.toString(), is(" WITH distinct targetTaxon, " +
                "interaction.label as iType, " +
                "sourceTaxon " +
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
                "NULL as study_title"));
    }

    private TreeMap<String, String[]> knownFields() {
        return new TreeMap<String, String[]>() {
            {
                put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            }
        };
    }

    private TreeMap<String, String[]> unknownFields() {
        return new TreeMap<String, String[]>() {
            {
                put("field", new String[]{"foo", "bar"});
            }
        };
    }

}