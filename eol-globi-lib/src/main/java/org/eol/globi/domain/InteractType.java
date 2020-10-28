package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.eol.globi.domain.InteractType.InteractionRole.OBJECT;
import static org.eol.globi.domain.InteractType.InteractionRole.SUBJECT;

public enum InteractType implements RelType {
    PREYS_UPON("http://purl.obolibrary.org/obo/RO_0002439", "preysOn", SUBJECT, OBJECT),
    PARASITE_OF("http://purl.obolibrary.org/obo/RO_0002444", "parasiteOf", SUBJECT, OBJECT),
    HAS_HOST("http://purl.obolibrary.org/obo/RO_0002454", "hasHost", SUBJECT, OBJECT),
    INTERACTS_WITH("http://purl.obolibrary.org/obo/RO_0002437", "interactsWith"),
    HOST_OF("http://purl.obolibrary.org/obo/RO_0002453", "hostOf", OBJECT, SUBJECT),
    POLLINATES("http://purl.obolibrary.org/obo/RO_0002455", "pollinates", SUBJECT, OBJECT),
    PERCHING_ON(PropertyAndValueDictionary.NO_MATCH, "perchingOn", SUBJECT, OBJECT),
    ATE("http://purl.obolibrary.org/obo/RO_0002470", "eats", SUBJECT, OBJECT),
    SYMBIONT_OF("http://purl.obolibrary.org/obo/RO_0002440", "symbiontOf"),
    PREYED_UPON_BY("http://purl.obolibrary.org/obo/RO_0002458", "preyedUponBy", OBJECT, SUBJECT),
    POLLINATED_BY("http://purl.obolibrary.org/obo/RO_0002456", "pollinatedBy", OBJECT, SUBJECT),
    EATEN_BY("http://purl.obolibrary.org/obo/RO_0002471", "eatenBy", OBJECT, SUBJECT),
    HAS_PARASITE("http://purl.obolibrary.org/obo/RO_0002445", "hasParasite", OBJECT, SUBJECT),
    PERCHED_ON_BY(PropertyAndValueDictionary.NO_MATCH, "perchedOnBy", OBJECT, SUBJECT),
    HAS_PATHOGEN("http://purl.obolibrary.org/obo/RO_0002557", "hasPathogen", OBJECT, SUBJECT),
    PATHOGEN_OF("http://purl.obolibrary.org/obo/RO_0002556", "pathogenOf", SUBJECT, OBJECT),

    HAS_VECTOR("http://purl.obolibrary.org/obo/RO_0002460", "hasVector", SUBJECT, OBJECT),
    VECTOR_OF("http://purl.obolibrary.org/obo/RO_0002459", "vectorOf", OBJECT, SUBJECT),

    VISITED_BY("http://purl.obolibrary.org/obo/RO_0002619", "visitedBy", OBJECT, SUBJECT),
    VISITS("http://purl.obolibrary.org/obo/RO_0002618", "visits", SUBJECT, OBJECT),

    FLOWERS_VISITED_BY("http://purl.obolibrary.org/obo/RO_0002623", "flowersVisitedBy", OBJECT, SUBJECT),
    VISITS_FLOWERS_OF("http://purl.obolibrary.org/obo/RO_0002622", "visitsFlowersOf", SUBJECT, OBJECT),

    INHABITED_BY(PropertyAndValueDictionary.NO_MATCH, "inhabitedBy"),
    INHABITS(PropertyAndValueDictionary.NO_MATCH, "inhabits"),

    ADJACENT_TO("http://purl.obolibrary.org/obo/RO_0002220", "adjacentTo"),

    CREATES_HABITAT_FOR("http://purl.obolibrary.org/obo/RO_0008505", "createsHabitatFor"),
    IS_HABITAT_OF(PropertyAndValueDictionary.NO_MATCH, "isHabitatOf"),

    LIVED_ON_BY(PropertyAndValueDictionary.NO_MATCH, "livedOnBy"),
    LIVES_ON(PropertyAndValueDictionary.NO_MATCH, "livesOn"),

    LIVED_INSIDE_OF_BY(PropertyAndValueDictionary.NO_MATCH, "livedInsideOfBy"),
    LIVES_INSIDE_OF(PropertyAndValueDictionary.NO_MATCH, "livesInsideOf"),

    LIVED_NEAR_BY(PropertyAndValueDictionary.NO_MATCH, "livedNearBy"),
    LIVES_NEAR(PropertyAndValueDictionary.NO_MATCH, "livesNear"),

    LIVED_UNDER_BY(PropertyAndValueDictionary.NO_MATCH, "livedUnderBy"),
    LIVES_UNDER(PropertyAndValueDictionary.NO_MATCH, "livesUnder"),

    LIVES_WITH(PropertyAndValueDictionary.NO_MATCH, "livesWith"),

    ENDOPARASITE_OF("http://purl.obolibrary.org/obo/RO_0002634", "endoparasiteOf", SUBJECT, OBJECT),
    HAS_ENDOPARASITE("http://purl.obolibrary.org/obo/RO_0002635", "hasEndoparasite", OBJECT, SUBJECT),

    HYPERPARASITE_OF("http://purl.obolibrary.org/obo/RO_0002553", "hyperparasiteOf", SUBJECT, OBJECT),
    HAS_HYPERPARASITE("http://purl.obolibrary.org/obo/RO_0002554", "hasHyperparasite", OBJECT, SUBJECT),

    HYPERPARASITOID_OF("http://purl.obolibrary.org/obo/RO_0002553", "hyperparasitoidOf", SUBJECT, OBJECT),
    HAS_HYPERPARASITOID("http://purl.obolibrary.org/obo/RO_0002554", "hasHyperparasitoid", OBJECT, SUBJECT),

    ECTOPARASITE_OF("http://purl.obolibrary.org/obo/RO_0002632", "ectoparasiteOf", SUBJECT, OBJECT),
    HAS_ECTOPARASITE("http://purl.obolibrary.org/obo/RO_0002633", "hasEctoparasite", OBJECT, SUBJECT),

    KLEPTOPARASITE_OF("http://purl.obolibrary.org/obo/RO_0008503", "kleptoparasiteOf", SUBJECT, OBJECT),
    HAS_KLEPTOPARASITE("http://purl.obolibrary.org/obo/RO_0008503", "hasKleptoparasite", OBJECT, SUBJECT),

    PARASITOID_OF("http://purl.obolibrary.org/obo/RO_0002208", "parasitoidOf", SUBJECT, OBJECT),
    HAS_PARASITOID("http://purl.obolibrary.org/obo/RO_0002209", "hasParasitoid", OBJECT, SUBJECT),

    ENDOPARASITOID_OF(PropertyAndValueDictionary.NO_MATCH, "endoparasitoidOf", SUBJECT, OBJECT),
    HAS_ENDOPARASITOID(PropertyAndValueDictionary.NO_MATCH, "hasEndoparasitoid", OBJECT, SUBJECT),

    ECTOPARASITOID_OF(PropertyAndValueDictionary.NO_MATCH, "ectoParasitoid", SUBJECT, OBJECT),
    HAS_ECTOPARASITOID(PropertyAndValueDictionary.NO_MATCH, "hasEctoparasitoid", OBJECT, SUBJECT),

    // living in something that is not the body.
    GUEST_OF(PropertyAndValueDictionary.NO_MATCH, "guestOf"),
    HAS_GUEST_OF(PropertyAndValueDictionary.NO_MATCH, "hasGuestOf"),

    FARMED_BY(PropertyAndValueDictionary.NO_MATCH, "farmedBy"),
    FARMS(PropertyAndValueDictionary.NO_MATCH, "farms"),

    DAMAGED_BY(PropertyAndValueDictionary.NO_MATCH, "damagedBy"),
    DAMAGES(PropertyAndValueDictionary.NO_MATCH, "damages"),

    DISPERSAL_VECTOR_OF(PropertyAndValueDictionary.NO_MATCH, "dispersalVectorOf"),
    HAS_DISPERAL_VECTOR(PropertyAndValueDictionary.NO_MATCH, "hasDispersalVector"),

    KILLED_BY("http://purl.obolibrary.org/obo/RO_0002627", "killedBy", OBJECT, SUBJECT),
    KILLS("http://purl.obolibrary.org/obo/RO_0002626", "kills", SUBJECT, OBJECT),

    EPIPHITE_OF("http://purl.obolibrary.org/obo/RO_0008501", "epiphyteOf", SUBJECT, OBJECT),
    HAS_EPIPHITE("http://purl.obolibrary.org/obo/RO_0008502", "hasEpiphyte", OBJECT, SUBJECT),

    LAYS_EGGS_ON("http://purl.obolibrary.org/obo/RO_0008507", "laysEggsOn", SUBJECT, OBJECT),
    HAS_EGGS_LAYED_ON_BY("http://purl.obolibrary.org/obo/RO_0008508", "hasEggsLayedOnBy", OBJECT, SUBJECT),

    CO_OCCURS_WITH("http://purl.obolibrary.org/obo/RO_0008506", "coOccursWith"),

    COMMENSALIST_OF("http://purl.obolibrary.org/obo/RO_0002441", "commensalistOf"),
    MUTUALIST_OF("http://purl.obolibrary.org/obo/RO_0002442", "mutualistOf"),

    AGGRESSOR_OF(PropertyAndValueDictionary.NO_MATCH, "aggressorOf"),
    HAS_AGGRESSOR(PropertyAndValueDictionary.NO_MATCH, "hasAggressor"),

    RELATED_TO("http://purl.obolibrary.org/obo/RO_0002321", "ecologicallyRelatedTo");


    String iri;
    String label;

    public enum InteractionRole {
        OBJECT,
        SUBJECT,
        NOT_DEFINED
    }

    InteractionRole sourceRole;
    InteractionRole targetRole;

    private static final Map<String, InteractType> SYNONYMS_OR_HYPORNYMS = new HashMap<String, InteractType>() {{
        put("http://eol.org/schema/terms/HasDispersalVector", HAS_VECTOR);
        put("http://eol.org/schema/terms/DispersalVector", VECTOR_OF);
        put("http://eol.org/schema/terms/FlowersVisitedBy", FLOWERS_VISITED_BY);
        put("http://eol.org/schema/terms/VisitsFlowersOf", VISITS_FLOWERS_OF);
        put("http://eol.org/schema/terms/kills", KILLS);
        put("http://eol.org/schema/terms/isKilledBy", KILLED_BY);
        put("http://eol.org/schema/terms/emergedFrom", INTERACTS_WITH);
        put("http://purl.obolibrary.org/obo/RO_0001025", INTERACTS_WITH);
    }};


    InteractType(String iri, String label) {
        this(iri, label, InteractionRole.NOT_DEFINED, InteractionRole.NOT_DEFINED);
    }

    InteractType(String iri, String label, InteractionRole sourceRole, InteractionRole targetRole) {
        this.iri = iri;
        this.label = StringUtils.isBlank(label) ? name() : label;
        this.sourceRole = sourceRole;
        this.targetRole = targetRole;
    }

    public String getIRI() {
        return iri;
    }

    public String getLabel() {
        return label;
    }

    public static InteractType typeOf(String iri) {
        if (StringUtils.startsWith(iri, "RO:")) {
            iri = StringUtils.replace(iri, "RO:", PropertyAndValueDictionary.RO_NAMESPACE);
        }
        InteractType[] values = values();
        for (InteractType interactType : values) {
            if (StringUtils.equalsIgnoreCase(iri, interactType.getIRI())) {
                return interactType;
            } else if (StringUtils.equalsIgnoreCase(iri, interactType.name())) {
                return interactType;
            } else if (StringUtils.equalsIgnoreCase(iri, interactType.getLabel())) {
                return interactType;
            }
        }
        return SYNONYMS_OR_HYPORNYMS.get(iri);
    }

    public static Collection<InteractType> hasTypes(InteractType type) {
        final Map<InteractType, Collection<InteractType>> pathMap = new HashMap<InteractType, Collection<InteractType>>() {
            {
                put(RELATED_TO, new ArrayList<>());
                put(INTERACTS_WITH, Arrays.asList(RELATED_TO, CO_OCCURS_WITH));
                put(CO_OCCURS_WITH, Collections.singletonList(RELATED_TO));
                put(ADJACENT_TO, Arrays.asList(RELATED_TO, CO_OCCURS_WITH));
                put(PERCHING_ON, Arrays.asList(LIVES_ON, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(ATE, Arrays.asList(INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(SYMBIONT_OF, Arrays.asList(INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(PREYS_UPON, Arrays.asList(ATE, KILLS, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(PATHOGEN_OF, Arrays.asList(PARASITE_OF, HAS_HOST, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(VECTOR_OF, Arrays.asList(HOST_OF, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(DISPERSAL_VECTOR_OF, Arrays.asList(HOST_OF, SYMBIONT_OF, INTERACTS_WITH, VECTOR_OF, RELATED_TO, CO_OCCURS_WITH));
                put(PARASITOID_OF, Arrays.asList(PARASITE_OF, HAS_HOST, ATE, KILLS, LIVES_WITH, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(ENDOPARASITOID_OF, Arrays.asList(PARASITOID_OF, PARASITE_OF, HAS_HOST, ATE, KILLS, LIVES_WITH, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(ECTOPARASITOID_OF, Arrays.asList(PARASITOID_OF, PARASITE_OF, HAS_HOST, ATE, KILLS, LIVES_WITH, LIVES_ON, ADJACENT_TO, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(HYPERPARASITOID_OF, Arrays.asList(PARASITOID_OF, PARASITE_OF, HAS_HOST, ATE, KILLS, LIVES_WITH, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(PARASITE_OF, Arrays.asList(ATE, DAMAGES, LIVES_WITH, HAS_HOST, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(HYPERPARASITE_OF, Arrays.asList(PARASITE_OF, ATE, DAMAGES, HAS_HOST, LIVES_WITH, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(ENDOPARASITE_OF, Arrays.asList(PARASITE_OF, LIVES_INSIDE_OF, HAS_HOST, ATE, DAMAGES, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(ECTOPARASITE_OF, Arrays.asList(PARASITE_OF, LIVES_ON, ADJACENT_TO, ATE, HAS_HOST, DAMAGES, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(POLLINATES, Arrays.asList(VISITS_FLOWERS_OF, HAS_HOST, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(VISITS, Arrays.asList(HAS_HOST, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(LAYS_EGGS_ON, Arrays.asList(HAS_HOST, INTERACTS_WITH, VISITS, RELATED_TO, CO_OCCURS_WITH));
                put(VISITS_FLOWERS_OF, Arrays.asList(HAS_HOST, INTERACTS_WITH, VISITS, RELATED_TO, CO_OCCURS_WITH));
                put(HOST_OF, Arrays.asList(SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(KLEPTOPARASITE_OF, Arrays.asList(INTERACTS_WITH, PARASITE_OF, SYMBIONT_OF, HAS_HOST, LIVES_WITH, ATE, DAMAGES, RELATED_TO, CO_OCCURS_WITH));
                put(INHABITS, Arrays.asList(INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(CREATES_HABITAT_FOR, Arrays.asList(INTERACTS_WITH, ADJACENT_TO, RELATED_TO, CO_OCCURS_WITH));
                put(LIVES_ON, Arrays.asList(INTERACTS_WITH, ADJACENT_TO, CREATES_HABITAT_FOR, RELATED_TO, CO_OCCURS_WITH));
                put(LIVES_INSIDE_OF, Arrays.asList(INTERACTS_WITH, CREATES_HABITAT_FOR, RELATED_TO, CO_OCCURS_WITH));
                put(LIVES_NEAR, Arrays.asList(INTERACTS_WITH, CREATES_HABITAT_FOR, RELATED_TO, CO_OCCURS_WITH));
                put(LIVES_UNDER, Arrays.asList(INTERACTS_WITH, CREATES_HABITAT_FOR, RELATED_TO, CO_OCCURS_WITH));
                put(LIVES_WITH, Arrays.asList(INTERACTS_WITH, CREATES_HABITAT_FOR, RELATED_TO, CO_OCCURS_WITH));
                put(GUEST_OF, Arrays.asList(INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(FARMS, Arrays.asList(ATE, SYMBIONT_OF, INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(DAMAGES, Arrays.asList(INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(KILLS, Arrays.asList(INTERACTS_WITH, RELATED_TO, CO_OCCURS_WITH));
                put(EPIPHITE_OF, Arrays.asList(INTERACTS_WITH, SYMBIONT_OF, RELATED_TO, CO_OCCURS_WITH));
                put(COMMENSALIST_OF, Arrays.asList(INTERACTS_WITH, SYMBIONT_OF, RELATED_TO, CO_OCCURS_WITH));
                put(MUTUALIST_OF, Arrays.asList(INTERACTS_WITH, SYMBIONT_OF, RELATED_TO, CO_OCCURS_WITH));
                put(AGGRESSOR_OF, Arrays.asList(RELATED_TO, CO_OCCURS_WITH));
            }
        };

        Map<InteractType, Collection<InteractType>> invertedPathMap = new HashMap<InteractType, Collection<InteractType>>() {
            {
                for (Map.Entry<InteractType, Collection<InteractType>> entry : pathMap.entrySet()) {
                    ArrayList<InteractType> invertedPath = new ArrayList<InteractType>();
                    InteractType keyInverse = inverseOf(entry.getKey());
                    if (keyInverse != entry.getKey()) {
                        for (InteractType interactType : entry.getValue()) {
                            InteractType inverse = inverseOf(interactType);
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
        return pathMap.get(type);
    }

    public static Collection<InteractType> typesOf(InteractType type) {
        Collection<InteractType> inversePath = new TreeSet<>();
        inversePath.add(type);
        for (InteractType interactType : values()) {
            if (hasTypes(interactType).contains(type)) {
                inversePath.add(interactType);
            }
        }
        return inversePath;
    }

    public static InteractType inverseOf(InteractType type) {
        Map<InteractType, InteractType> inverseMap = new HashMap<InteractType, InteractType>() {
            {
                put(POLLINATES, POLLINATED_BY);
                put(PATHOGEN_OF, HAS_PATHOGEN);
                put(VECTOR_OF, HAS_VECTOR);
                put(FLOWERS_VISITED_BY, VISITS_FLOWERS_OF);
                put(LAYS_EGGS_ON, HAS_EGGS_LAYED_ON_BY);
                put(VISITS, VISITED_BY);
                put(INHABITED_BY, INHABITS);
                put(FARMED_BY, FARMS);
                put(CREATES_HABITAT_FOR, IS_HABITAT_OF);
                put(LIVED_ON_BY, LIVES_ON);
                put(LIVED_INSIDE_OF_BY, LIVES_INSIDE_OF);
                put(LIVED_NEAR_BY, LIVES_NEAR);
                put(LIVED_UNDER_BY, LIVES_UNDER);
                put(LIVES_WITH, LIVES_WITH);
                put(KLEPTOPARASITE_OF, HAS_KLEPTOPARASITE);
                put(GUEST_OF, HAS_GUEST_OF);
                put(PERCHING_ON, PERCHED_ON_BY);
                put(HOST_OF, HAS_HOST);
                put(PREYS_UPON, PREYED_UPON_BY);
                put(ATE, EATEN_BY);
                put(DAMAGED_BY, DAMAGES);
                put(KILLS, KILLED_BY);
                put(SYMBIONT_OF, SYMBIONT_OF);
                put(INTERACTS_WITH, INTERACTS_WITH);
                put(CO_OCCURS_WITH, CO_OCCURS_WITH);
                put(ADJACENT_TO, ADJACENT_TO);
                put(PARASITE_OF, HAS_PARASITE);
                put(HYPERPARASITE_OF, HAS_HYPERPARASITE);
                put(ENDOPARASITE_OF, HAS_ENDOPARASITE);
                put(ECTOPARASITE_OF, HAS_ECTOPARASITE);
                put(PARASITOID_OF, HAS_PARASITOID);
                put(HYPERPARASITOID_OF, HAS_HYPERPARASITOID);
                put(ENDOPARASITOID_OF, HAS_ENDOPARASITOID);
                put(ECTOPARASITOID_OF, HAS_ECTOPARASITOID);
                put(DISPERSAL_VECTOR_OF, HAS_DISPERAL_VECTOR);
                put(EPIPHITE_OF, HAS_EPIPHITE);
                put(COMMENSALIST_OF, COMMENSALIST_OF);
                put(MUTUALIST_OF, MUTUALIST_OF);
                put(RELATED_TO, RELATED_TO);
                put(AGGRESSOR_OF, HAS_AGGRESSOR);
            }
        };

        final Map<InteractType, InteractType> swappedMap = new HashMap<InteractType, InteractType>();
        for (Map.Entry<InteractType, InteractType> entry : inverseMap.entrySet()) {
            swappedMap.put(entry.getValue(), entry.getKey());
        }
        inverseMap.putAll(swappedMap);

        return inverseMap.get(type);
    }


}
