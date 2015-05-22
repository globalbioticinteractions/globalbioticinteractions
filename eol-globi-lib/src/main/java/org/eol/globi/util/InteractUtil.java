package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;

import java.util.HashMap;
import java.util.Map;

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

    public static InteractType inverseOf(InteractType interactType) {
        Map<InteractType, InteractType> inverseMap = new HashMap<InteractType, InteractType>() {
            {
                put(InteractType.SYMBIONT_OF, InteractType.SYMBIONT_OF);

                put(InteractType.INTERACTS_WITH, InteractType.INTERACTS_WITH);

                put(InteractType.PREYS_UPON, InteractType.PREYED_UPON_BY);
                put(InteractType.PREYED_UPON_BY, InteractType.PREYS_UPON);

                put(InteractType.POLLINATES, InteractType.POLLINATED_BY);
                put(InteractType.POLLINATED_BY, InteractType.POLLINATES);

                put(InteractType.ATE, InteractType.EATEN_BY);
                put(InteractType.EATEN_BY, InteractType.ATE);

                put(InteractType.PARASITE_OF, InteractType.HAS_PARASITE);
                put(InteractType.HAS_PARASITE, InteractType.PARASITE_OF);

                put(InteractType.HAS_HOST, InteractType.HOST_OF);
                put(InteractType.HOST_OF, InteractType.HAS_HOST);

                put(InteractType.PERCHING_ON, InteractType.PERCHED_ON_BY);
                put(InteractType.PERCHED_ON_BY, InteractType.PERCHING_ON);

                put(InteractType.PATHOGEN_OF, InteractType.HAS_PATHOGEN);
                put(InteractType.HAS_PATHOGEN, InteractType.PATHOGEN_OF);

                put(InteractType.VECTOR_OF, InteractType.HAS_VECTOR);
                put(InteractType.HAS_VECTOR, InteractType.VECTOR_OF);
            }
        };
        return inverseMap.get(interactType);
    }

}
