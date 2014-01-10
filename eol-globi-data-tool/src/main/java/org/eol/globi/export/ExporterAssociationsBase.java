package org.eol.globi.export;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;

import java.util.HashMap;
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

    public static final String DEFAULT_INTERACT_TYPE = "http://eol.org/schema/terms/interactsWith";
    private static final Map<String, String> GLOBI_EOL_INTERACT_MAP = new HashMap<String, String>() {
        {
            put(InteractType.ATE.name(), "http://eol.org/schema/terms/eats");
            put(InteractType.INTERACTS_WITH.name(), DEFAULT_INTERACT_TYPE);
            put(InteractType.HOST_OF.name(), "http://eol.org/schema/terms/hosts");
            put(InteractType.PREYS_UPON.name(), "http://eol.org/schema/terms/preysUpon");
            put(InteractType.HAS_HOST.name(), "http://eol.org/schema/terms/hasHost");
            put(InteractType.POLLINATES.name(), "http://eol.org/schema/terms/pollinates");
            put(InteractType.PERCHING_ON.name(), "http://eol.org/schema/terms/isFoundOn");
            put(InteractType.PARASITE_OF.name(), "http://eol.org/schema/terms/parasitizes");
        }
    };

    protected void addStudyInfo(Study study, Map<String, String> properties) {
        properties.put(EOLDictionary.SOURCE, study.getSource());
        properties.put(EOLDictionary.REFERENCE_ID, "globi:ref:" + study.getNodeID());
    }

    protected String getEOLTermFor(String interactionType) {
        String eolInteractTerm = GLOBI_EOL_INTERACT_MAP.get(interactionType);
        return eolInteractTerm == null ? DEFAULT_INTERACT_TYPE : eolInteractTerm;
    }
}
