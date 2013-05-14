package org.eol.globi.util;

import org.eol.globi.domain.InteractType;

public class InteractUtil {
    public static String allInteractionsCypherClause() {
        InteractType[] values = InteractType.values();
        StringBuilder interactions = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            interactions.append(i == 0 ? "" : "|");
            interactions.append(values[i]);
        }
        return interactions.toString();
    }
}
