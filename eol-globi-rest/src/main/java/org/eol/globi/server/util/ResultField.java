package org.eol.globi.server.util;

public enum ResultField {
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    ALTITUDE("altitude"),
    COLLECTION_TIME_IN_UNIX_EPOCH("collection_time_in_unix_epoch"),
    STUDY_TITLE("study_title"),
    INTERACTION_TYPE("interaction_type"),

    TARGET_TAXON_NAME("target_taxon_name"),
    SOURCE_TAXON_NAME("source_taxon_name"),

    TARGET_TAXON_COMMON_NAMES("target_taxon_common_names"),
    SOURCE_TAXON_COMMON_NAMES("source_taxon_common_names"),

    TARGET_TAXON_EXTERNAL_ID("target_taxon_external_id"),
    SOURCE_TAXON_EXTERNAL_ID("source_taxon_external_id"),

    TARGET_TAXON_PATH("target_taxon_path"),
    SOURCE_TAXON_PATH("source_taxon_path"),

    TARGET_TAXON_PATH_RANKS("target_taxon_path_ranks"),
    SOURCE_TAXON_PATH_RANKS("source_taxon_path_ranks"),

    TARGET_TAXON_PATH_IDS("target_taxon_path_ids"),
    SOURCE_TAXON_PATH_IDS("source_taxon_path_ids"),

    TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE("target_specimen_frequency_of_occurrence"),
    TARGET_SPECIMEN_TOTAL_VOLUME_ML("target_specimen_total_volume_ml"),
    TARGET_SPECIMEN_TOTAL_COUNT("target_specimen_total_count"),

    TARGET_SPECIMEN_ID("tmp_and_unique_target_specimen_id"),
    SOURCE_SPECIMEN_ID("tmp_and_unique_source_specimen_id"),

    TARGET_SPECIMEN_PHYSIOLOGICAL_STATE("target_specimen_physiological_state"),
    SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE( "source_specimen_physiological_state"),

    TARGET_SPECIMEN_BODY_PART("target_specimen_body_part"),
    SOURCE_SPECIMEN_BODY_PART("source_specimen_body_part"),

    SOURCE_SPECIMEN_LIFE_STAGE("source_specimen_life_stage"),
    TARGET_SPECIMEN_LIFE_STAGE("target_specimen_life_stage"),

    SOURCE_SPECIMEN_BASIS_OF_RECORD("source_specimen_basis_of_record"),
    TARGET_SPECIMEN_BASIS_OF_RECORD("target_specimen_basis_of_record"),

    TAXON_NAME("taxon_name"),
    TAXON_COMMON_NAMES("taxon_common_names"),
    TAXON_EXTERNAL_ID("taxon_external_id"),
    TAXON_PATH("taxon_path"),
    TAXON_PATH_IDS("taxon_path_ids"),
    TAXON_PATH_RANKS("taxon_path_ranks"),
    STUDY_URL("study_url"),
    STUDY_DOI("study_doi"),
    STUDY_CITATION("study_citation"),
    STUDY_SOURCE_CITATION("study_source_citation");

    public String getLabel() {
        return label;
    }

    private final String label;

    private ResultField(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

    public String getDescription() {
        return "a description of " + getLabel();
    }
}
