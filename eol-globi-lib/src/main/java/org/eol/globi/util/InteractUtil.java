package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class InteractUtil {

    public static String allInteractionsCypherClause() {
        return interactionsCypherClause(InteractType.values());
    }

    public static String joinInteractTypes(Collection<InteractType> interactTypes) {
        return StringUtils.join(interactTypes, CharsetConstant.SEPARATOR_CHAR);
    }

    public static String interactionsCypherClause(InteractType... values) {
        TreeSet<InteractType> types = new TreeSet<>();
        for (InteractType value : values) {
            types.addAll(InteractType.typesOf(value));
        }
        return joinInteractTypes(types);
    }

}
