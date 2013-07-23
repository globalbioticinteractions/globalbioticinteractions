package org.eol.globi.export;

public abstract class ExporterOccurrencesBase extends ExporterBase {
    protected String[] getFields() {
        return new String[]{
                EOLDictionary.OCCURRENCE_ID,
                EOLDictionary.TAXON_ID,
                EOLDictionary.SEX,
                EOLDictionary.LIFE_STAGE,
                // TODO reuse term physiological state from existing ontology
                EOLDictionary.PHYSIOLOGICAL_STATE,
                // TODO reuse term body part from existing ontology
                EOLDictionary.BODY_PART,
                EOLDictionary.REPRODUCTIVE_CONDITION,
                EOLDictionary.BEHAVIOR,
                EOLDictionary.ESTABLISHMENT_MEANS,
                EOLDictionary.OCCURRENCE_REMARKS,
                EOLDictionary.INDIVIDUAL_COUNT,
                EOLDictionary.PREPARATIONS,
                EOLDictionary.FIELD_NOTES,
                EOLDictionary.SAMPLING_PROTOCOL,
                EOLDictionary.SAMPLING_EFFORT,
                EOLDictionary.DECIMAL_LATITUDE,
                EOLDictionary.DECIMAL_LONGITUDE,
                // TODO reuse term depth from existing ontology
                EOLDictionary.DEPTH,
                // TODO reuse term depth from existing ontology
                EOLDictionary.ALTITUDE,
                EOLDictionary.LOCALITY,
                EOLDictionary.IDENTIFIED_BY,
                EOLDictionary.DATE_IDENTIFIED,
                EOLDictionary.EVENT_DATE,
                EOLDictionary.EVENT_ID
        };
    }
}
