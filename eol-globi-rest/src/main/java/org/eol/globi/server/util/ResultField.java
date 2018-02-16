package org.eol.globi.server.util;

public enum ResultField {
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    ALTITUDE("altitude"),
    FOOTPRINT_WKT("footprintWKT"),
    LOCALITY("locality"),
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
    TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE_PERCENT("target_specimen_frequency_of_occurrence_percent"),
    TARGET_SPECIMEN_TOTAL_VOLUME_ML("target_specimen_total_volume_ml"),
    TARGET_SPECIMEN_TOTAL_VOLUME_PERCENT("target_specimen_total_volume_ml_percent"),
    TARGET_SPECIMEN_TOTAL_COUNT("target_specimen_total_count"),
    TARGET_SPECIMEN_TOTAL_COUNT_PERCENT("target_specimen_total_count_percent"),

    TARGET_SPECIMEN_ID("tmp_and_unique_target_specimen_id"),
    SOURCE_SPECIMEN_ID("tmp_and_unique_source_specimen_id"),

    TARGET_SPECIMEN_PHYSIOLOGICAL_STATE("target_specimen_physiological_state"),
    TARGET_SPECIMEN_PHYSIOLOGICAL_STATE_ID("target_specimen_physiological_state_id"),
    SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE( "source_specimen_physiological_state"),
    SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE_ID( "source_specimen_physiological_state_id"),

    TARGET_SPECIMEN_BODY_PART("target_specimen_body_part"),
    TARGET_SPECIMEN_BODY_PART_ID("target_specimen_body_part_id"),
    SOURCE_SPECIMEN_BODY_PART("source_specimen_body_part"),
    SOURCE_SPECIMEN_BODY_PART_ID("source_specimen_body_part_id"),

    SOURCE_SPECIMEN_LIFE_STAGE("source_specimen_life_stage"),
    SOURCE_SPECIMEN_LIFE_STAGE_ID("source_specimen_life_stage_id"),
    TARGET_SPECIMEN_LIFE_STAGE("target_specimen_life_stage"),
    TARGET_SPECIMEN_LIFE_STAGE_ID("target_specimen_life_stage_id"),

    SOURCE_SPECIMEN_BASIS_OF_RECORD("source_specimen_basis_of_record"),
    TARGET_SPECIMEN_BASIS_OF_RECORD("target_specimen_basis_of_record"),

    TAXON_NAME("taxon_name"),
    TAXON_COMMON_NAMES("taxon_common_names"),
    TAXON_EXTERNAL_ID("taxon_external_id"),

    TAXON_EXTERNAL_URL("taxon_external_url"),
    TAXON_PATH("taxon_path"),
    TAXON_PATH_IDS("taxon_path_ids"),
    TAXON_PATH_RANKS("taxon_path_ranks"),
    STUDY_URL("study_url"),
    STUDY_DOI("study_doi"),
    STUDY_CITATION("study_citation"),
    STUDY_SOURCE_CITATION("study_source_citation"),
    STUDY_SOURCE_ID("study_source_id"),
    NUMBER_OF_DISTINCT_TAXA("number_of_distinct_taxa", "only available for /reports/* queries"),
    NUMBER_OF_DISTINCT_TAXA_NO_MATCH("number_of_distinct_taxa_no_match", "only available for /reports/* queries"),
    NUMBER_OF_SOURCES("number_of_sources", "only available for /reports/* queries"),
    NUMBER_OF_STUDIES("number_of_studies", "only available for /reports/* queries"),
    NUMBER_OF_INTERACTIONS("number_of_interactions", "available for /interaction queries by source/target taxon name and/or interactionType only"),
    STUDY_SOURCE_DOI("study_source_doi"),
    STUDY_SOURCE_FORMAT("study_source_format"),
    STUDY_SOURCE_ARCHIVE_URI("study_source_archive_uri"),
    STUDY_SOURCE_LAST_SEEN_AT("study_source_last_seen_at");

    public String getLabel() {
        return label;
    }

    private final String label;
    private final String description;

    ResultField(String label) {
        this(label, "a description of " + label);
    }

    ResultField(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String toString() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
