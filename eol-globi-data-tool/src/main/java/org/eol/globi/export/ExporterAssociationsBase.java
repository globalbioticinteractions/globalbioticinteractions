package org.eol.globi.export;

import org.eol.globi.domain.Study;

import java.util.Map;

public abstract class ExporterAssociationsBase extends ExporterBase {
    protected String[] getFields() {
        return new String[]{
                EOLDictionary.ASSOCIATION_ID,
                EOLDictionary.OCCURRENCE_ID,
                EOLDictionary.ASSOCIATION_TYPE,
                EOLDictionary.TARGET_OCCURRENCE_ID,
                EOLDictionary.SOURCE,
                EOLDictionary.REFERENCE_ID
        };
    }

    protected void addStudyInfo(Study study, Map<String, String> properties) {
        properties.put(EOLDictionary.SOURCE, study.getSource());
        properties.put(EOLDictionary.REFERENCE_ID, "globi:ref:" + study.getNodeID());
    }
}
