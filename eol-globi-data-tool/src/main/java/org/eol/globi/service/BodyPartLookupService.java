package org.eol.globi.service;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.BodyPart;

import java.util.HashMap;
import java.util.Map;

public class BodyPartLookupService {

    public static final Map<String,BodyPart> BODY_PART_MAP = new HashMap<String, BodyPart>() {
        {
            put("NA", BodyPart.UNKNOWN);
            put("parts", BodyPart.UNKNOWN);
            put("pieces", BodyPart.UNKNOWN);
            put("fragments", BodyPart.UNKNOWN);
            put("scales", BodyPart.SCALES);
            put("scale(s)", BodyPart.SCALES);
            put("bone", BodyPart.BONE);
            put("tube", BodyPart.TUBE);
            put("spine", BodyPart.SPINE);
            put("pollen grain", BodyPart.POLLEN_GRAIN);
            put("opercula", BodyPart.OPERCULUM);
            put("caudal spines", BodyPart.CAUDAL_SPINE);
            put("shell", BodyPart.SHELL);
            put("needles", BodyPart.NEEDLE);
            put("seed pod", BodyPart.SEED_POD);
            put("kernels", BodyPart.KERNEL);
            put("wood", BodyPart.WOOD);
            put("feather", BodyPart.FEATHER);
        }
    };

    public BodyPart lookup(String bodyPartName) throws LookupServiceException {
        BodyPart bodyPart = null;
        if (bodyPartName != null && bodyPartName.trim().length() > 0) {
            bodyPart = BODY_PART_MAP.get(bodyPartName);
            if (bodyPart == null) {
                throw new LookupServiceException("unknown bodypart [" + bodyPartName + "]");
            }
        }
        return bodyPart;
    }
}
