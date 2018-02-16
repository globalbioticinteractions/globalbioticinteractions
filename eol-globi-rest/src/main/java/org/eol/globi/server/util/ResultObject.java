package org.eol.globi.server.util;

import java.util.Map;
import java.util.TreeMap;

public enum ResultObject {

    SOURCE_TAXON("sourceTaxon"),
    SOURCE_TAXON_DISTINCT("sourceTaxon"),
    SOURCE_SPECIMEN("sourceSpecimen"),

    INTERACTION("interaction"),
    INTERACTION_TYPE("iType"),
    INTERACTION_COUNT("interactionCount"),



    COLLECTED_REL("collected_rel"),

    TARGET_TAXON("targetTaxon"),
    TARGET_TAXON_DISTINCT("targetTaxon"),
    TARGET_SPECIMEN("targetSpecimen"),

    LOCATION("loc"),
    STUDY("study"),
    DATASET("dataset"),
    STUDY_COUNT("studyCount"),
    STUDY_SOURCE_COUNT("sourceCount"),

    TAXON("taxon");

    public static ResultObject forField(ResultField field) {
        Map<ResultField, ResultObject> fieldToObject = new TreeMap<ResultField, ResultObject>() {{
            put(ResultField.LATITUDE, LOCATION);
            put(ResultField.LONGITUDE, LOCATION);
            put(ResultField.LOCALITY, LOCATION);
            put(ResultField.FOOTPRINT_WKT, LOCATION);
            put(ResultField.ALTITUDE, LOCATION);


            put(ResultField.SOURCE_TAXON_NAME, SOURCE_TAXON);
            put(ResultField.SOURCE_TAXON_COMMON_NAMES, SOURCE_TAXON);
            put(ResultField.SOURCE_TAXON_EXTERNAL_ID, SOURCE_TAXON);
            put(ResultField.SOURCE_TAXON_PATH, SOURCE_TAXON);
            put(ResultField.SOURCE_TAXON_PATH_IDS, SOURCE_TAXON);
            put(ResultField.SOURCE_TAXON_PATH_RANKS, SOURCE_TAXON);

            put(ResultField.SOURCE_SPECIMEN_BASIS_OF_RECORD, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_BODY_PART, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_BODY_PART_ID, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_ID, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_LIFE_STAGE, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_LIFE_STAGE_ID, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE, SOURCE_SPECIMEN);
            put(ResultField.SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE_ID, SOURCE_SPECIMEN);

            put(ResultField.TARGET_TAXON_NAME, TARGET_TAXON);
            put(ResultField.TARGET_TAXON_COMMON_NAMES, TARGET_TAXON);
            put(ResultField.TARGET_TAXON_EXTERNAL_ID, TARGET_TAXON);
            put(ResultField.TARGET_TAXON_PATH, TARGET_TAXON);
            put(ResultField.TARGET_TAXON_PATH_IDS, TARGET_TAXON);
            put(ResultField.TARGET_TAXON_PATH_RANKS, TARGET_TAXON);

            put(ResultField.TARGET_SPECIMEN_BASIS_OF_RECORD, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_BODY_PART, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_BODY_PART_ID, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_ID, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_LIFE_STAGE, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_LIFE_STAGE_ID, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_PHYSIOLOGICAL_STATE, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_PHYSIOLOGICAL_STATE_ID, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE_PERCENT, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_TOTAL_VOLUME_ML, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_TOTAL_VOLUME_PERCENT, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_TOTAL_COUNT, TARGET_SPECIMEN);
            put(ResultField.TARGET_SPECIMEN_TOTAL_COUNT_PERCENT, TARGET_SPECIMEN);

            put(ResultField.STUDY_CITATION, STUDY);
            put(ResultField.STUDY_DOI, STUDY);
            put(ResultField.STUDY_SOURCE_CITATION, STUDY);
            put(ResultField.STUDY_SOURCE_ID, STUDY);
            put(ResultField.STUDY_TITLE, STUDY);
            put(ResultField.STUDY_URL, STUDY);

            put(ResultField.STUDY_SOURCE_DOI, DATASET);
            put(ResultField.STUDY_SOURCE_FORMAT, DATASET);
            put(ResultField.STUDY_SOURCE_ARCHIVE_URI, DATASET);
            put(ResultField.STUDY_SOURCE_LAST_SEEN_AT, DATASET);

            put(ResultField.COLLECTION_TIME_IN_UNIX_EPOCH, COLLECTED_REL);
            put(ResultField.INTERACTION_TYPE, INTERACTION);

            put(ResultField.TAXON_NAME, TAXON);
            put(ResultField.TAXON_COMMON_NAMES, TAXON);
            put(ResultField.TAXON_EXTERNAL_ID, TAXON);
            put(ResultField.TAXON_EXTERNAL_URL, TAXON);
            put(ResultField.TAXON_PATH, TAXON);
            put(ResultField.TAXON_PATH_IDS, TAXON);
            put(ResultField.TAXON_PATH_RANKS, TAXON);

            put(ResultField.NUMBER_OF_DISTINCT_TAXA, TAXON);
            put(ResultField.NUMBER_OF_DISTINCT_TAXA_NO_MATCH, TAXON);
            put(ResultField.NUMBER_OF_SOURCES, STUDY);
            put(ResultField.NUMBER_OF_STUDIES, STUDY);

            put(ResultField.NUMBER_OF_INTERACTIONS, INTERACTION);
        }};
        return fieldToObject.get(field);
    }

    public String getLabel() {
        return label;
    }

    private final String label;

    ResultObject(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

}
