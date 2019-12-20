package org.eol.globi.util;

import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.InteractType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class InteractUtil {

    private static final Set<String> UNLIKELY_INTERACTION_TYPE_NAMES
            = UnmodifiableSet.unmodifiableSet(new TreeSet<String>() {{
        add("(collected with)");
        add("collector number");
        add("(in amplexus with)");
        add("(littermate or nestmate of)");
        add("(mate of)");
        add("mixed species flock");
        add("mosses");
        add("(offspring of)");
        add("(parent of)");
        add("(same individual as)");
        add("same litter");
        add("(same lot as)");
        add("(sibling of)");
    }});

    private static final Map<String, InteractType> INTERACTION_TYPE_NAME_MAP =
            UnmodifiableMap.unmodifiableMap(new HashMap<String, InteractType>() {{
                put("associated with", InteractType.RELATED_TO);
                put("ex", InteractType.HAS_HOST);
                put("ex.", InteractType.HAS_HOST);
                put("reared ex", InteractType.HAS_HOST);
                put("reared ex.", InteractType.HAS_HOST);
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
            }});

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

    public static boolean ignoredInteractionTypeName(String interactionTypeNameCandidate) {
        return StringUtils.isBlank(interactionTypeNameCandidate)
                ? false
                : UNLIKELY_INTERACTION_TYPE_NAMES
                .contains(StringUtils.lowerCase(interactionTypeNameCandidate));

    }

    public static InteractType getInteractTypeForName(String interactionName) {
        InteractType interactType = InteractType.typeOf(interactionName);
        return interactType != null
                ? interactType
                : INTERACTION_TYPE_NAME_MAP.get(interactionName);
    }
}
