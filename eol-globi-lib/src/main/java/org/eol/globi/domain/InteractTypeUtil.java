package org.eol.globi.domain;

import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class InteractTypeUtil {


    static Map<InteractType, Collection<InteractType>> initTypeOfPath() {
        Map<InteractType, Collection<InteractType>> typeOfPath = new TreeMap<>();
        InteractType[] values = InteractType.values();
        for (InteractType value : values) {
            Collection<InteractType> typePath = new TreeSet<>();
            typePath.add(value);
            for (InteractType interactType : InteractType.values()) {
                if (InteractType.hasTypes(interactType).contains(value)) {
                    typePath.add(interactType);
                }
            }
            typeOfPath.put(value, typePath);
        }
        return MapUtils.unmodifiableMap(typeOfPath);

    }

    static Map<InteractType, InteractType> initInverseOfPath() {
        Map<InteractType, InteractType> inverseMap = new LinkedHashMap<InteractType, InteractType>() {
            {
                put(InteractType.POLLINATES, InteractType.POLLINATED_BY);
                put(InteractType.PATHOGEN_OF, InteractType.HAS_PATHOGEN);
                put(InteractType.VECTOR_OF, InteractType.HAS_VECTOR);
                put(InteractType.FLOWERS_VISITED_BY, InteractType.VISITS_FLOWERS_OF);
                put(InteractType.LAYS_EGGS_ON, InteractType.HAS_EGGS_LAYED_ON_BY);
                put(InteractType.LAYS_EGGS_IN, InteractType.HAS_EGGS_LAYED_IN_BY);
                put(InteractType.VISITS, InteractType.VISITED_BY);
                put(InteractType.INHABITED_BY, InteractType.INHABITS);
                put(InteractType.FARMED_BY, InteractType.FARMS);
                put(InteractType.CREATES_HABITAT_FOR, InteractType.HAS_HABITAT);
                put(InteractType.LIVED_ON_BY, InteractType.LIVES_ON);
                put(InteractType.LIVED_INSIDE_OF_BY, InteractType.LIVES_INSIDE_OF);
                put(InteractType.LIVED_NEAR_BY, InteractType.LIVES_NEAR);
                put(InteractType.LIVED_UNDER_BY, InteractType.LIVES_UNDER);
                put(InteractType.LIVES_WITH, InteractType.LIVES_WITH);
                put(InteractType.KLEPTOPARASITE_OF, InteractType.HAS_KLEPTOPARASITE);
                put(InteractType.GUEST_OF, InteractType.HAS_GUEST_OF);
                put(InteractType.PERCHING_ON, InteractType.PERCHED_ON_BY);
                put(InteractType.HOST_OF, InteractType.HAS_HOST);
                put(InteractType.RESERVOIR_HOST_OF, InteractType.HAS_RESERVOIR_HOST);
                put(InteractType.PREYS_UPON, InteractType.PREYED_UPON_BY);
                put(InteractType.ATE, InteractType.EATEN_BY);
                put(InteractType.ACQUIRES_NUTRIENTS_FROM, InteractType.PROVIDES_NUTRIENTS_FOR);
                put(InteractType.DAMAGED_BY, InteractType.DAMAGES);
                put(InteractType.KILLS, InteractType.KILLED_BY);
                put(InteractType.SYMBIONT_OF, InteractType.SYMBIONT_OF);
                put(InteractType.INTERACTS_WITH, InteractType.INTERACTS_WITH);
                put(InteractType.TROPHICALLY_INTERACTS_WITH, InteractType.TROPHICALLY_INTERACTS_WITH);
                put(InteractType.CO_OCCURS_WITH, InteractType.CO_OCCURS_WITH);
                put(InteractType.CO_ROOSTS_WITH, InteractType.CO_ROOSTS_WITH);
                put(InteractType.HAS_ROOST, InteractType.ROOST_OF);
                put(InteractType.ADJACENT_TO, InteractType.ADJACENT_TO);
                put(InteractType.PARASITE_OF, InteractType.HAS_PARASITE);
                put(InteractType.HYPERPARASITE_OF, InteractType.HAS_HYPERPARASITE);
                put(InteractType.ENDOPARASITE_OF, InteractType.HAS_ENDOPARASITE);
                put(InteractType.ECTOPARASITE_OF, InteractType.HAS_ECTOPARASITE);
                put(InteractType.PARASITOID_OF, InteractType.HAS_PARASITOID);
                put(InteractType.ENDOPARASITOID_OF, InteractType.HAS_ENDOPARASITOID);
                put(InteractType.ECTOPARASITOID_OF, InteractType.HAS_ECTOPARASITOID);
                put(InteractType.DISPERSAL_VECTOR_OF, InteractType.HAS_DISPERAL_VECTOR);
                put(InteractType.EPIPHITE_OF, InteractType.HAS_EPIPHITE);
                put(InteractType.COMMENSALIST_OF, InteractType.COMMENSALIST_OF);
                put(InteractType.MUTUALIST_OF, InteractType.MUTUALIST_OF);
                put(InteractType.RELATED_TO, InteractType.RELATED_TO);
                put(InteractType.AGGRESSOR_OF, InteractType.HAS_AGGRESSOR);
                put(InteractType.ALLELOPATH_OF, InteractType.HAS_ALLELOPATH);
                put(InteractType.HEMIPARASITE_OF, InteractType.HAS_PARASITE);
                put(InteractType.ROOTPARASITE_OF, InteractType.HAS_PARASITE);
            }
        };

        Map<InteractType, InteractType> swappedMap = new LinkedHashMap<>();
        for (Map.Entry<InteractType, InteractType> entry : inverseMap.entrySet()) {
            swappedMap.putIfAbsent(entry.getValue(), entry.getKey());
        }
        inverseMap.putAll(swappedMap);
        return MapUtils.unmodifiableMap(inverseMap);
    }

    static Map<InteractType, Collection<InteractType>> initHasTypesPath() {
        Map<InteractType, Collection<InteractType>> pathMap = new HashMap<InteractType, Collection<InteractType>>() {
            {
                put(InteractType.RELATED_TO, new ArrayList<>());
                put(InteractType.INTERACTS_WITH, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.TROPHICALLY_INTERACTS_WITH, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH, InteractType.INTERACTS_WITH));
                put(InteractType.CO_OCCURS_WITH, Collections.singletonList(InteractType.RELATED_TO));
                put(InteractType.CO_ROOSTS_WITH, Arrays.asList(InteractType.CO_OCCURS_WITH, InteractType.RELATED_TO));
                put(InteractType.HAS_ROOST, Arrays.asList(InteractType.CO_OCCURS_WITH, InteractType.RELATED_TO, InteractType.INTERACTS_WITH));
                put(InteractType.ADJACENT_TO, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.PERCHING_ON, Arrays.asList(InteractType.LIVES_ON, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.ATE, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.ACQUIRES_NUTRIENTS_FROM, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.PROVIDES_NUTRIENTS_FOR, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.SYMBIONT_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.PREYS_UPON, Arrays.asList(InteractType.ATE, InteractType.KILLS, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.PATHOGEN_OF, Arrays.asList(InteractType.HAS_HOST, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.VECTOR_OF, Arrays.asList(InteractType.HOST_OF, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.DISPERSAL_VECTOR_OF, Arrays.asList(InteractType.HOST_OF, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.VECTOR_OF, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.PARASITOID_OF, Arrays.asList(InteractType.PARASITE_OF, InteractType.HAS_HOST, InteractType.ATE, InteractType.KILLS, InteractType.LIVES_WITH, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.ENDOPARASITOID_OF, Arrays.asList(InteractType.PARASITOID_OF, InteractType.PARASITE_OF, InteractType.HAS_HOST, InteractType.ATE, InteractType.KILLS, InteractType.LIVES_WITH, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.ECTOPARASITOID_OF, Arrays.asList(InteractType.PARASITOID_OF, InteractType.PARASITE_OF, InteractType.HAS_HOST, InteractType.ATE, InteractType.KILLS, InteractType.LIVES_WITH, InteractType.LIVES_ON, InteractType.ADJACENT_TO, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.PARASITE_OF, Arrays.asList(InteractType.ATE, InteractType.DAMAGES, InteractType.LIVES_WITH, InteractType.HAS_HOST, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.HYPERPARASITE_OF, Arrays.asList(InteractType.PARASITE_OF, InteractType.ATE, InteractType.DAMAGES, InteractType.HAS_HOST, InteractType.LIVES_WITH, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.ENDOPARASITE_OF, Arrays.asList(InteractType.PARASITE_OF, InteractType.LIVES_INSIDE_OF, InteractType.HAS_HOST, InteractType.ATE, InteractType.DAMAGES, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.ECTOPARASITE_OF, Arrays.asList(InteractType.PARASITE_OF, InteractType.LIVES_ON, InteractType.ADJACENT_TO, InteractType.ATE, InteractType.HAS_HOST, InteractType.DAMAGES, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.POLLINATES, Arrays.asList(InteractType.VISITS_FLOWERS_OF, InteractType.HAS_HOST, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.VISITS, Arrays.asList(InteractType.HAS_HOST, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LAYS_EGGS_ON, Arrays.asList(InteractType.HAS_HOST, InteractType.INTERACTS_WITH, InteractType.VISITS, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LAYS_EGGS_IN, Arrays.asList(InteractType.HAS_HOST, InteractType.INTERACTS_WITH, InteractType.VISITS, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.VISITS_FLOWERS_OF, Arrays.asList(InteractType.HAS_HOST, InteractType.INTERACTS_WITH, InteractType.VISITS, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.HOST_OF, Arrays.asList(InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.RESERVOIR_HOST_OF, Arrays.asList(InteractType.HOST_OF, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.KLEPTOPARASITE_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.PARASITE_OF, InteractType.SYMBIONT_OF, InteractType.HAS_HOST, InteractType.LIVES_WITH, InteractType.ATE, InteractType.DAMAGES, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.INHABITS, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.CREATES_HABITAT_FOR, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.ADJACENT_TO, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.HAS_HABITAT, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.ADJACENT_TO, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LIVES_ON, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.ADJACENT_TO, InteractType.CREATES_HABITAT_FOR, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LIVES_INSIDE_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.CREATES_HABITAT_FOR, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LIVES_NEAR, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.CREATES_HABITAT_FOR, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LIVES_UNDER, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.CREATES_HABITAT_FOR, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.LIVES_WITH, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.CREATES_HABITAT_FOR, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.GUEST_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.FARMS, Arrays.asList(InteractType.ATE, InteractType.SYMBIONT_OF, InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.DAMAGES, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.KILLS, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.EPIPHITE_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.SYMBIONT_OF, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.COMMENSALIST_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.SYMBIONT_OF, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.MUTUALIST_OF, Arrays.asList(InteractType.INTERACTS_WITH, InteractType.SYMBIONT_OF, InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.AGGRESSOR_OF, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH));
                put(InteractType.HEMIPARASITE_OF, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH, InteractType.INTERACTS_WITH, InteractType.SYMBIONT_OF, InteractType.PARASITE_OF));
                put(InteractType.ROOTPARASITE_OF, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH, InteractType.INTERACTS_WITH, InteractType.SYMBIONT_OF, InteractType.PARASITE_OF));
                put(InteractType.ALLELOPATH_OF, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH, InteractType.INTERACTS_WITH));
                put(InteractType.HAS_ALLELOPATH, Arrays.asList(InteractType.RELATED_TO, InteractType.CO_OCCURS_WITH, InteractType.INTERACTS_WITH));
            }
        };

        Map<InteractType, Collection<InteractType>> invertedPathMap = new HashMap<InteractType, Collection<InteractType>>() {
            {
                for (Entry<InteractType, Collection<InteractType>> entry : pathMap.entrySet()) {
                    ArrayList<InteractType> invertedPath = new ArrayList<>();
                    InteractType keyInverse = InteractType.inverseOf(entry.getKey());
                    if (keyInverse != entry.getKey()) {
                        for (InteractType interactType : entry.getValue()) {
                            InteractType inverse = InteractType.inverseOf(interactType);
                            if (null != inverse) {
                                invertedPath.add(inverse);
                            }
                        }
                        put(keyInverse, invertedPath);
                    }
                }
            }
        };
        pathMap.putAll(invertedPathMap);
        return MapUtils.unmodifiableMap(pathMap);
    }
}
