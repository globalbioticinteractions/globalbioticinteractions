package org.eol.globi.server.util;

public class ResultFields {
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";

    public static final String COLLECTION_TIME_IN_UNIX_EPOCH = "collection_time_in_unix_epoch";
    public static final String STUDY_TITLE = "study_title";
    public static final String INTERACTION_TYPE = "interaction_type";

    public static final String TARGET_TAXON_NAME = "target_taxon_name";
    public static final String TARGET_TAXON_COMMON_NAMES = "target_taxon_common_names";
    public static final String TARGET_TAXON_EXTERNAL_ID = "target_taxon_external_id";
    public static final String TARGET_TAXON_PATH = "target_taxon_path";
    public static final String TARGET_TAXON_PATH_RANKS = "target_taxon_path_ranks";
    public static final String TARGET_TAXON_PATH_IDS = "target_taxon_path_ids";

    public static final String SOURCE_TAXON_NAME = "source_taxon_name";
    public static final String SOURCE_TAXON_COMMON_NAMES = "source_taxon_common_names";

    public static final String SOURCE_TAXON_EXTERNAL_ID = "source_taxon_external_id";
    public static final String SOURCE_TAXON_PATH = "source_taxon_path";
    public static final String SOURCE_TAXON_PATH_RANKS = "source_taxon_path_ranks";
    public static final String SOURCE_TAXON_PATH_IDS = "source_taxon_path_ids";

    public static final String PREFIX_TARGET_SPECIMEN = "target_specimen";
    public static final String TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE = PREFIX_TARGET_SPECIMEN + "_frequency_of_occurrence";
    public static final String TARGET_SPECIMEN_TOTAL_VOLUME_ML = PREFIX_TARGET_SPECIMEN + "_total_volume_ml";
    public static final String TARGET_SPECIMEN_TOTAL_COUNT = PREFIX_TARGET_SPECIMEN + "_total_count";
    public static final String TARGET_SPECIMEN_ID = "tmp_and_unique_" + PREFIX_TARGET_SPECIMEN + "_id";
    public static final String PREFIX_SOURCE_SPECIMEN = "source_specimen";
    public static final String SOURCE_SPECIMEN_ID = "tmp_and_unique_" + PREFIX_SOURCE_SPECIMEN + "_id";

    public static final String SUFFIX_PHYSIOLOGICAL_STATE = "_physiological_state";
    public static final String TARGET_SPECIMEN_PHYSIOLOGICAL_STATE = PREFIX_TARGET_SPECIMEN + SUFFIX_PHYSIOLOGICAL_STATE;
    public static final String SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE = PREFIX_SOURCE_SPECIMEN + SUFFIX_PHYSIOLOGICAL_STATE;
    public static final String SUFFIX_BODY_PART = "_body_part";
    public static final String TARGET_SPECIMEN_BODY_PART = PREFIX_TARGET_SPECIMEN + SUFFIX_BODY_PART;
    public static final String SOURCE_SPECIMEN_BODY_PART = PREFIX_SOURCE_SPECIMEN + SUFFIX_BODY_PART;
    public static final String SUFFIX_LIFE_STAGE = "_life_stage";
    public static final String SOURCE_SPECIMEN_LIFE_STAGE = PREFIX_SOURCE_SPECIMEN + SUFFIX_LIFE_STAGE;
    public static final String TARGET_SPECIMEN_LIFE_STAGE = PREFIX_TARGET_SPECIMEN + SUFFIX_LIFE_STAGE;


    public static final String TAXON_NAME = "taxon_name";
    public static final String TAXON_COMMON_NAMES = "taxon_common_names";
    public static final String TAXON_EXTERNAL_ID = "taxon_external_id";
    public static final String TAXON_PATH = "taxon_path";
    public static final String TAXON_PATH_IDS = "taxon_path_ids";
    public static final String TAXON_PATH_RANKS = "taxon_path_ranks";
}
