package org.eol.globi.service;

import org.eol.globi.domain.PhysiologicalState;

import java.util.HashMap;
import java.util.Map;

public class PhysiologicalStateLookupService {

    public static final Map<String,PhysiologicalState> PHYSIOLOGICAL_STATE_MAP = new HashMap<String, PhysiologicalState>() {
        {
            put("NA", PhysiologicalState.UNKNOWN);
            put("remains", PhysiologicalState.REMAINS);
            put("digestate", PhysiologicalState.DIGESTATE);
        }
    };

    public PhysiologicalState lookup(String physiologicalStateName) throws LookupServiceException {
        PhysiologicalState physiologicalState = null;
        if (physiologicalStateName != null && physiologicalStateName.trim().length() > 0) {
            physiologicalState = PHYSIOLOGICAL_STATE_MAP.get(physiologicalStateName);
            if (physiologicalState == null) {
                throw new LookupServiceException("unknown physiological state [" + physiologicalStateName + "]");
            }
        }
        return physiologicalState;
    }
}
