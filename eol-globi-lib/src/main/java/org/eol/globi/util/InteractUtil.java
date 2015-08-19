package org.eol.globi.util;

import org.eol.globi.domain.InteractType;

import java.util.HashMap;
import java.util.Map;

public class InteractUtil {

    public static String allInteractionsCypherClause() {
        return interactionsCypherClause(InteractType.values());
    }

    protected static String interactionsCypherClause(InteractType[] values) {
        StringBuilder interactions = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            interactions.append(i == 0 ? "" : "|");
            interactions.append(values[i]);
        }
        return interactions.toString();
    }

}
