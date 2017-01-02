package org.eol.globi.export;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;

import java.util.Map;

public abstract class ExporterAssociationsBase extends ExporterBase {
    protected String[] getFields() {
        return new String[]{
                EOLDictionary.ASSOCIATION_ID,
                EOLDictionary.OCCURRENCE_ID,
                EOLDictionary.ASSOCIATION_TYPE,
                EOLDictionary.TARGET_OCCURRENCE_ID,
                EOLDictionary.MEASUREMENT_DETERMINED_DATE,
                EOLDictionary.MEASUREMENT_DETERMINED_BY,
                EOLDictionary.MEASUREMENT_METHOD,
                EOLDictionary.MEASUREMENT_REMARKS,
                EOLDictionary.SOURCE,
                EOLDictionary.BIBLIOGRAPHIC_CITATION,
                EOLDictionary.CONTRIBUTOR,
                EOLDictionary.REFERENCE_ID
        };
    }

    @Override
    protected String getRowType() {
        return "http://eol.org/schema/Association";
    }

    protected void addStudyInfo(Study study, Map<String, String> properties) {
        properties.put(EOLDictionary.SOURCE, study.getSource());
        properties.put(EOLDictionary.REFERENCE_ID, "globi:ref:" + ((StudyNode)study).getNodeID());
    }

    protected String getEOLTermFor(String interactionType) {
        InteractType interactType = InteractType.valueOf(interactionType);
        return (interactType == null || PropertyAndValueDictionary.NO_MATCH.equals(interactType.getIRI()))
                ? InteractType.INTERACTS_WITH.getIRI()
                : interactType.getIRI();
    }
}
