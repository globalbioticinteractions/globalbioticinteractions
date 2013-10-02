package org.eol.globi.service;

import org.eol.globi.data.LifeStage;

import java.util.HashMap;
import java.util.Map;

public class LifeStageLookupService {

    public static final Map<String,LifeStage> LIFE_STAGE_MAP = new HashMap<String, LifeStage>() {
        {
            put("adult", LifeStage.ADULT);
            put("adults", LifeStage.ADULT);
            put("a", LifeStage.ADULT);
            put("juvenile", LifeStage.JUVENILE);
            put("juvenile?", LifeStage.JUVENILE);
            put("j", LifeStage.JUVENILE);
            put("nd", LifeStage.UNKNOWN);
            put("na", LifeStage.UNKNOWN);
            put("zoea", LifeStage.ZOEA);
            put("larvae", LifeStage.LARVA);
            put("larval", LifeStage.LARVA);
            put("post larvae", LifeStage.POSTLARVA);
            put("post-larvae", LifeStage.POSTLARVA);
            put("l/pl", LifeStage.LARVA_OR_POSTLARVA);
            put("larvae/post larvae", LifeStage.LARVA_OR_POSTLARVA);
            put("megalopa", LifeStage.MEGALOPA);
            put("cypris", LifeStage.CYPRIS);
            put("cyp", LifeStage.CYPRIS);
            put("egg", LifeStage.EGG);
            put("eggs", LifeStage.EGG);
            put("nauplii", LifeStage.NAUPLII);
            put("copepodite", LifeStage.COPEPODITE);
            put("copepodites", LifeStage.COPEPODITE);
            put("copepedites", LifeStage.COPEPODITE);
            put("glaucothoe", LifeStage.GLAUCOTHOE);
            put("veligers", LifeStage.VELIGER);
            put("newborn", LifeStage.NEWBORN);

        }
    };

    public LifeStage lookup(String lifeStageName) throws LookupServiceException {
        LifeStage lifeStage = null;
        if (lifeStageName != null && lifeStageName.trim().length() > 0) {
            lifeStage = LIFE_STAGE_MAP.get(lifeStageName.toLowerCase());
            if (lifeStage == null) {
                throw new LookupServiceException("unknown life stage [" + lifeStageName + "]");
            }
        }
        return lifeStage;
    }
}
