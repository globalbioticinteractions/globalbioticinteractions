package org.eol.globi.server.util;

import org.eol.globi.domain.InteractType;
import org.eol.globi.server.CypherQueryBuilder;

public enum InteractionTypeExternal {
    EATS(CypherQueryBuilder.INTERACTION_EATS, "consumer", "food", InteractType.ATE),
    EATEN_BY(CypherQueryBuilder.INTERACTION_EATEN_BY, "food", "consumer", InteractType.EATEN_BY),
    PREYS_ON(CypherQueryBuilder.INTERACTION_PREYS_ON, "predator", "prey", InteractType.PREYS_UPON),
    PREYED_UPON_BY(CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "prey", "predator", InteractType.PREYED_UPON_BY),
    KILLS(CypherQueryBuilder.INTERACTION_KILLS, "killer", "victim", InteractType.KILLS),
    KILLED_BY(CypherQueryBuilder.INTERACTION_KILLED_BY, "victim", "killer", InteractType.KILLED_BY),
    PARASITE_OF(CypherQueryBuilder.INTERACTION_PARASITE_OF, "parasite", "host", InteractType.PARASITE_OF),
    HAS_PARASITE(CypherQueryBuilder.INTERACTION_HAS_PARASITE, "host", "parasite", InteractType.HAS_PARASITE),
    ENDOPARASITE_OF(CypherQueryBuilder.INTERACTION_ENDOPARASITE_OF, "endoparasite", "host", InteractType.ENDOPARASITE_OF),
    HAS_ENDOPARASITE(CypherQueryBuilder.INTERACTION_HAS_ENDOPARASITE, "host", "endoparasite", InteractType.HAS_ENDOPARASITE),
    ECTOPARASITE_OF(CypherQueryBuilder.INTERACTION_ECTOPARASITE_OF, "ectoparasite", "host", InteractType.ECTOPARASITE_OF),
    HAS_ECTOPARASITE(CypherQueryBuilder.INTERACTION_HAS_ECTOPARASITE, "host", "ectoparasite", InteractType.HAS_ECTOPARASITE),
    PARASITOID_OF(CypherQueryBuilder.INTERACTION_PARASITOID_OF, "parasitoid", "host", InteractType.PARASITOID_OF),
    HAS_PARASITOID(CypherQueryBuilder.INTERACTION_HAS_PARASITOID, "host", "parasitoid", InteractType.HAS_PARASITOID),
    HOST_OF(CypherQueryBuilder.INTERACTION_HOST_OF, "host", "symbiont", InteractType.HOST_OF),
    HAS_HOST(CypherQueryBuilder.INTERACTION_HAS_HOST, "symbiont", "host", InteractType.HAS_HOST),
    POLLINATES(CypherQueryBuilder.INTERACTION_POLLINATES, "pollinator", "plant", InteractType.POLLINATES),
    POLLINATED_BY(CypherQueryBuilder.INTERACTION_POLLINATED_BY, "plant", "pollinator", InteractType.POLLINATED_BY),
    PATHOGEN_OF(CypherQueryBuilder.INTERACTION_PATHOGEN_OF, "pathogen", "host", InteractType.PATHOGEN_OF),
    ALLELOPATH_OF(InteractType.ALLELOPATH_OF.getLabel(), "pathogen", "host", InteractType.ALLELOPATH_OF),
    HAS_PATHOGEN(CypherQueryBuilder.INTERACTION_HAS_PATHOGEN, "host", "pathogen", InteractType.HAS_PATHOGEN),
    VECTOR_OF(CypherQueryBuilder.INTERACTION_VECTOR_OF, "vector", "pathogen", InteractType.VECTOR_OF),
    HAS_VECTOR(CypherQueryBuilder.INTERACTION_HAS_VECTOR, "pathogen", "vector", InteractType.HAS_VECTOR),
    DISPERSAL_VECTOR_OF(CypherQueryBuilder.INTERACTION_DISPERSAL_VECTOR_OF, "vector", "seed", InteractType.DISPERSAL_VECTOR_OF),
    HAS_DISPERSAL_VECTOR(CypherQueryBuilder.INTERACTION_HAS_DISPERSAL_VECTOR, "seed", "vector", InteractType.HAS_DISPERAL_VECTOR),
    ROOT_PARASITE_OF(CypherQueryBuilder.ROOT_PARASITE_OF, "parasite", "plantRoot", InteractType.ROOTPARASITE_OF),
    HEMI_PARASITE_OF(CypherQueryBuilder.HEMI_PARASITE_OF, "parasite", "plant", InteractType.HEMIPARASITE_OF),

    HAS_HABITAT(CypherQueryBuilder.INTERACTION_HAS_HABITAT, "inhabitant", "habitat", InteractType.HAS_HABITAT),
    CREATES_HABITAT_FOR(CypherQueryBuilder.INTERACTION_CREATES_HABITAT_FOR, "habitat", "inhabitant", InteractType.CREATES_HABITAT_FOR),

    EPIPHITE_OF(InteractType.EPIPHITE_OF.getLabel(), "plant/algae", "host plant", InteractType.EPIPHITE_OF),
    HAS_EPIPHITE(InteractType.HAS_EPIPHITE.getLabel(), "plant", "plant/algae", InteractType.HAS_EPIPHITE),

    PROVIDES_NUTRIENTS_FOR(InteractType.PROVIDES_NUTRIENTS_FOR.getLabel(), "host", "consumer", InteractType.PROVIDES_NUTRIENTS_FOR),
    ACQUIRES_NUTRIENTS_FROM(InteractType.ACQUIRES_NUTRIENTS_FROM.getLabel(), "consumer", "host", InteractType.ACQUIRES_NUTRIENTS_FROM),

    SYMBIONT_OF(CypherQueryBuilder.INTERACTION_SYMBIONT_OF, "symbiont", "symbiont", InteractType.SYMBIONT_OF),
    MUTUALIST_OF(CypherQueryBuilder.INTERACTION_MUTUALIST_OF, "mutualist", "mutualist", InteractType.MUTUALIST_OF),
    COMMENSALIST_OF(CypherQueryBuilder.INTERACTION_COMMENSALIST_OF, "commensalist", "commensalist", InteractType.COMMENSALIST_OF),
    FLOWERS_VISITED_BY(CypherQueryBuilder.INTERACTION_FLOWERS_VISITED_BY, "plant", "visitor", InteractType.FLOWERS_VISITED_BY),
    VISITS_FLOWERS_OF(CypherQueryBuilder.INTERACTION_VISITS_FLOWERS_OF, "visitor", "plant", InteractType.VISITS_FLOWERS_OF),
    RELATED_TO(CypherQueryBuilder.INTERACTION_RELATED_TO, "source", "target", InteractType.RELATED_TO),
    CO_OCCURS_WITH(CypherQueryBuilder.INTERACTION_CO_OCCURS_WITH, "source", "target", InteractType.CO_OCCURS_WITH),
    CO_ROOSTS_WITH(CypherQueryBuilder.INTERACTION_CO_ROOSTS_WITH, "source", "target", InteractType.CO_ROOSTS_WITH),
    INTERACTS_WITH(CypherQueryBuilder.INTERACTION_INTERACTS_WITH, "source", "target", InteractType.INTERACTS_WITH),
    ADJACENT_TO(CypherQueryBuilder.INTERACTION_ADJACENT_TO, "source", "target", InteractType.ADJACENT_TO);

    InteractionTypeExternal(String label, String source, String target, InteractType interactType) {
        this.label = label;
        this.source = source;
        this.target = target;
        this.interactType = interactType;
    }

    public String getLabel() {
        return label;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getTermIRI() {
        return interactType.getIRI();
    }

    private String label;
    private String source;
    private String target;
    private final InteractType interactType;


}
