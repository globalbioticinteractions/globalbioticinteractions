package org.eol.globi.export;

public abstract class ExporterAssociationsBase extends ExporterBase {
    protected String[] getFields() {
        return new String[]{
                EOLDictionary.ASSOCIATION_ID,
                EOLDictionary.OCCURRENCE_ID,
                EOLDictionary.ASSOCIATION_TYPE,
                EOLDictionary.TARGET_OCCURRENCE_ID,
                EOLDictionary.SOURCE
        };
    }
}
