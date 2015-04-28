package org.eol.globi.export;

public abstract class ExporterOccurrencesBase extends ExporterBase {

    protected String[] getFields() {
        return new String[]{
                EOLDictionary.OCCURRENCE_ID,
                EOLDictionary.TAXON_ID,
                EOLDictionary.INSTITUTION_CODE,
                EOLDictionary.COLLECTION_CODE,
                EOLDictionary.CATALOG_NUMBER,
                EOLDictionary.SEX,
                EOLDictionary.LIFE_STAGE,
                EOLDictionary.REPRODUCTIVE_CONDITION,
                EOLDictionary.BEHAVIOR,
                EOLDictionary.ESTABLISHMENT_MEANS,
                EOLDictionary.OCCURRENCE_REMARKS,
                EOLDictionary.INDIVIDUAL_COUNT,
                EOLDictionary.PREPARATIONS,
                EOLDictionary.FIELD_NOTES,
                EOLDictionary.BASIS_OF_RECORD,
                EOLDictionary.SAMPLING_PROTOCOL,
                EOLDictionary.SAMPLING_EFFORT,
                EOLDictionary.IDENTIFIED_BY,
                EOLDictionary.DATE_IDENTIFIED,
                EOLDictionary.EVENT_DATE,
                "http://purl.org/dc/terms/modified",
                EOLDictionary.LOCALITY,
                EOLDictionary.DECIMAL_LATITUDE,
                EOLDictionary.DECIMAL_LONGITUDE,
                "http://rs.tdwg.org/dwc/terms/verbatimLatitude",
                "http://rs.tdwg.org/dwc/terms/verbatimLongitude",
                EOLDictionary.VERBATIM_ELEVATION,
                // TODO reuse term physiological state from existing ontology
                EOLDictionary.PHYSIOLOGICAL_STATE,
                // TODO reuse term body part from existing ontology
                EOLDictionary.BODY_PART,
        };
    }

    @Override
    protected String getRowType() {
        return "http://rs.tdwg.org/dwc/terms/Occurrence";
    }
}
