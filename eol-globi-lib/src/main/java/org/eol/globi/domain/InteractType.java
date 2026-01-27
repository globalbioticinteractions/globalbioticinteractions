package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.InteractType.InteractionRole.OBJECT;
import static org.eol.globi.domain.InteractType.InteractionRole.SUBJECT;

public enum InteractType implements RelType {
    PREYS_UPON("http://purl.obolibrary.org/obo/RO_0002439", "preysOn", SUBJECT, OBJECT),
    PARASITE_OF("http://purl.obolibrary.org/obo/RO_0002444", "parasiteOf", SUBJECT, OBJECT),
    HAS_HOST("http://purl.obolibrary.org/obo/RO_0002454", "hasHost", SUBJECT, OBJECT),
    HAS_RESERVOIR_HOST("http://purl.obolibrary.org/obo/RO_0002803", "hasReservoirHost", SUBJECT, OBJECT),
    INTERACTS_WITH("http://purl.obolibrary.org/obo/RO_0002437", "interactsWith"),
    TROPHICALLY_INTERACTS_WITH("http://purl.obolibrary.org/obo/RO_0002438", "trophicallyInteractsWith"),
    HOST_OF("http://purl.obolibrary.org/obo/RO_0002453", "hostOf", OBJECT, SUBJECT),
    RESERVOIR_HOST_OF("http://purl.obolibrary.org/obo/RO_0002802", "reservoirHostOf", OBJECT, SUBJECT),
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

    ACQUIRES_NUTRIENTS_FROM("http://purl.obolibrary.org/obo/RO_0002457", "acquiresNutrientsFrom", OBJECT, SUBJECT),
    PROVIDES_NUTRIENTS_FOR("http://purl.obolibrary.org/obo/RO_0002469", "providesNutrientsFor", SUBJECT, OBJECT),

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
    HAS_HABITAT("http://purl.obolibrary.org/obo/RO_0002303", "hasHabitat"),

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

    ECTOPARASITE_OF("http://purl.obolibrary.org/obo/RO_0002632", "ectoparasiteOf", SUBJECT, OBJECT),
    HAS_ECTOPARASITE("http://purl.obolibrary.org/obo/RO_0002633", "hasEctoparasite", OBJECT, SUBJECT),

    KLEPTOPARASITE_OF("http://purl.obolibrary.org/obo/RO_0008503", "kleptoparasiteOf", SUBJECT, OBJECT),
    HAS_KLEPTOPARASITE("http://purl.obolibrary.org/obo/RO_0008504", "hasKleptoparasite", OBJECT, SUBJECT),

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

    LAYS_EGGS_IN("http://purl.obolibrary.org/obo/RO_0002624", "laysEggsIn", SUBJECT, OBJECT),
    HAS_EGGS_LAYED_IN_BY("http://purl.obolibrary.org/obo/RO_0002625", "hasEggsLayedInBy", OBJECT, SUBJECT),

    CO_OCCURS_WITH("http://purl.obolibrary.org/obo/RO_0008506", "coOccursWith"),
    CO_ROOSTS_WITH("http://purl.obolibrary.org/obo/RO_0002801", "coRoostsWith"),

    HAS_ROOST("http://purl.obolibrary.org/obo/RO_0008509", "hasRoost", SUBJECT, OBJECT),
    ROOST_OF(PropertyAndValueDictionary.NO_MATCH, "roostOf", OBJECT, SUBJECT),

    COMMENSALIST_OF("http://purl.obolibrary.org/obo/RO_0002441", "commensalistOf"),
    MUTUALIST_OF("http://purl.obolibrary.org/obo/RO_0002442", "mutualistOf"),

    AGGRESSOR_OF(PropertyAndValueDictionary.NO_MATCH, "aggressorOf"),
    HAS_AGGRESSOR(PropertyAndValueDictionary.NO_MATCH, "hasAggressor"),

    ALLELOPATH_OF("http://purl.obolibrary.org/obo/RO_0002555", "allelopathOf", SUBJECT, OBJECT),
    HAS_ALLELOPATH("http://purl.obolibrary.org/obo/RO_0020301", "hasAllelopath", OBJECT, SUBJECT),

    HEMIPARASITE_OF("http://purl.obolibrary.org/obo/RO_0002237", "hemiparasiteOf", SUBJECT, OBJECT),
    ROOTPARASITE_OF("http://purl.obolibrary.org/obo/RO_0002236", "rootparasiteOf", SUBJECT, OBJECT),

    HAS_ECTOMYCORRYZAL_HOST("http://purl.obolibrary.org/obo/RO_0002805", "hasEctomycorrhizalHost", SUBJECT, OBJECT),
    ECTOMYCORRYZAL_HOST_OF("http://purl.obolibrary.org/obo/RO_0002804", "ectomycorrhizalHostOf", SUBJECT, OBJECT),

    HAS_ARBUSCULAR_MYCORRYZAL_HOST("http://purl.obolibrary.org/obo/RO_0002807", "hasArbuscularMycorrhizalHost", SUBJECT, OBJECT),
    ARBUSCULAR_MYCORRYZAL_HOST_OF("http://purl.obolibrary.org/obo/RO_0002806", "arbuscularMycorrhizalHostOf", SUBJECT, OBJECT),

    RELATED_TO("http://purl.obolibrary.org/obo/RO_0002321", "ecologicallyRelatedTo");


    String iri;
    String label;


    private final static Map<InteractType, InteractType> inverseOfPath
            = InteractTypeUtil.initInverseOfPath();

    private final static Map<InteractType, Collection<InteractType>> hasTypesPath
            = InteractTypeUtil.initHasTypesPath();

    private final static Map<InteractType, Collection<InteractType>> typeOfPath
            = InteractTypeUtil.initTypeOfPath();

    public enum InteractionRole {
        OBJECT,
        SUBJECT,
        NOT_DEFINED
    }

    InteractionRole sourceRole;
    InteractionRole targetRole;

    private static final Map<String, InteractType> SYNONYMS_OR_HYPONYMS = new HashMap<String, InteractType>() {{
        put("http://eol.org/schema/terms/HasDispersalVector", HAS_VECTOR);
        put("http://eol.org/schema/terms/DispersalVector", VECTOR_OF);
        put("http://eol.org/schema/terms/FlowersVisitedBy", FLOWERS_VISITED_BY);
        put("http://eol.org/schema/terms/VisitsFlowersOf", VISITS_FLOWERS_OF);
        put("http://eol.org/schema/terms/kills", KILLS);
        put("http://eol.org/schema/terms/isKilledBy", KILLED_BY);
        put("http://eol.org/schema/terms/emergedFrom", INTERACTS_WITH);
        put("http://purl.obolibrary.org/obo/RO_0001025", INTERACTS_WITH);
        put("http://purl.obolibrary.org/obo/RO_0002574", INTERACTS_WITH);
        put("http://purl.obolibrary.org/obo/RO_0002434", INTERACTS_WITH);
        put("hasHyperparasitoid", HAS_HYPERPARASITE);
        put("hyperparasitoidOf", HYPERPARASITE_OF);
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
        iri = replaceWithRONamespace(iri, "RO:");
        iri = replaceWithRONamespace(iri, "RO_");

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
        InteractType interactType = SYNONYMS_OR_HYPONYMS.get(iri);

        if (interactType == null) {
            String iriTrimmed = StringUtils.trim(iri);
            if (!StringUtils.equals(iri, iriTrimmed)) {
                return typeOf(iriTrimmed);
            }
        }

        return interactType;
    }

    private static String replaceWithRONamespace(String iri, String prefix) {
        if (StringUtils.startsWith(iri, prefix)) {
            iri = StringUtils.replace(iri, prefix, PropertyAndValueDictionary.RO_NAMESPACE);
        }
        return iri;
    }

    public static Collection<InteractType> hasTypes(InteractType type) {
        return hasTypesPath.get(type);
    }

    public static Collection<InteractType> typesOf(InteractType type) {
        return typeOfPath.get(type);
    }

    public static InteractType inverseOf(InteractType type) {
        return inverseOfPath.get(type);
    }


}
