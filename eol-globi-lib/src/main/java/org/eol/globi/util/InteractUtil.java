package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.InteractType;

import java.util.Collection;
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
