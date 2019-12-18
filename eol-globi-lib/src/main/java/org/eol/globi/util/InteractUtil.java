package org.eol.globi.util;

import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.InteractType;

import java.util.Collection;
import java.util.HashMap;
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

    public static InteractType getInteractTypeForName(String interactionName) {
        InteractType interactType = InteractType.typeOf(interactionName);
        return interactType != null
                ? interactType
                : UnmodifiableMap.unmodifiableMap(new HashMap<String, InteractType>() {{
            put("associated with", InteractType.RELATED_TO);
            put("ex", InteractType.HAS_HOST);
            put("host to", InteractType.HOST_OF);
            put("host", InteractType.HAS_HOST);
            put("h", InteractType.HAS_HOST);
            put("larval foodplant", InteractType.ATE);
            put("ectoparasite of", InteractType.ECTOPARASITE_OF);
            put("parasite of", InteractType.PARASITE_OF);
            put("stomach contents of", InteractType.EATEN_BY);
            put("stomach contents", InteractType.ATE);
            put("eaten by", InteractType.EATEN_BY);
            put("(ate)", InteractType.ATE);
            put("(eaten by)", InteractType.EATEN_BY);
            put("(parasite of)", InteractType.PARASITE_OF);
            put("(host of)", InteractType.HOST_OF);
            put("consumption", InteractType.ATE);
            put("flower predator", InteractType.ATE);
            put("flower visitor", InteractType.VISITS_FLOWERS_OF);
            put("folivory", InteractType.ATE);
            put("fruit thief", InteractType.ATE);
            put("ingestion", InteractType.ATE);
            put("pollinator", InteractType.POLLINATES);
            put("seed disperser", InteractType.DISPERSAL_VECTOR_OF);
            put("seed predator", InteractType.ATE);
            put("n/a", null);
            put("neutral", null);
            put("unknown", null);
        }}).get(interactionName);
    }
}
