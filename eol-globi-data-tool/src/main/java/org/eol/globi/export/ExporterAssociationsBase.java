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
                EOLDictionary.SOURCE,
                EOLDictionary.REFERENCE_ID
        };
    }

    public static final String DEFAULT_INTERACT_TYPE = "/interactsWith";
    private static final Map<String, String> GLOBI_EOL_INTERACT_MAP = new HashMap<String, String>() {
        {
            put(InteractType.ATE.name(), "/eats");
            put(InteractType.INTERACTS_WITH.name(), DEFAULT_INTERACT_TYPE);
            put(InteractType.HOST_OF.name(), "/hosts");
            put(InteractType.PREYS_UPON.name(), "/preysUpon");
            put(InteractType.HAS_HOST.name(), "/hasHost");
            put(InteractType.POLLINATES.name(), "/pollinates");
            put(InteractType.PERCHING_ON.name(), "/isFoundOn");
            put(InteractType.PARASITE_OF.name(), "/parasitizes");
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
