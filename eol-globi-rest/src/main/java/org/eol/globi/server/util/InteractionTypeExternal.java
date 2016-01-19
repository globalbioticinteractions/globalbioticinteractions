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
    HOST_OF(CypherQueryBuilder.INTERACTION_HOST_OF, "host", "symbiont", InteractType.HOST_OF),
    HAS_HOST(CypherQueryBuilder.INTERACTION_HAS_HOST, "symbiont", "host", InteractType.HAS_HOST),
    POLLINATES(CypherQueryBuilder.INTERACTION_POLLINATES, "pollinator", "plant", InteractType.POLLINATES),
    POLLINATED_BY(CypherQueryBuilder.INTERACTION_POLLINATED_BY, "plant", "pollinator", InteractType.POLLINATED_BY),
    PATHOGEN_OF(CypherQueryBuilder.INTERACTION_PATHOGEN_OF, "pathogen", "host", InteractType.PATHOGEN_OF),
    HAS_PATHOGEN(CypherQueryBuilder.INTERACTION_HAS_PATHOGEN, "host", "pathogen", InteractType.HAS_PATHOGEN),
    VECTOR_OF(CypherQueryBuilder.INTERACTION_VECTOR_OF, "vector", "pathogen", InteractType.VECTOR_OF),
    HAS_VECTOR(CypherQueryBuilder.INTERACTION_HAS_VECTOR, "pathogen", "vector", InteractType.HAS_VECTOR),
    DISPERSAL_VECTOR_OF(CypherQueryBuilder.INTERACTION_DISPERSAL_VECTOR_OF, "vector", "seed", InteractType.DISPERSAL_VECTOR_OF),
    HAS_DISPERSAL_VECTOR(CypherQueryBuilder.INTERACTION_HAS_DISPERSAL_VECTOR, "seed", "vector", InteractType.HAS_DISPERAL_VECTOR),

    SYMBIONT_OF(CypherQueryBuilder.INTERACTION_SYMBIONT_OF, "source", "target", InteractType.SYMBIONT_OF),
    FLOWERS_VISITED_BY(CypherQueryBuilder.INTERACTION_FLOWERS_VISITED_BY, "plant", "visitor", InteractType.FLOWERS_VISITED_BY),
    VISITS_FLOWERS_OF(CypherQueryBuilder.INTERACTION_VISITS_FLOWERS_OF, "visitor", "plant", InteractType.VISITS_FLOWERS_OF),
    INTERACTS_WITH(CypherQueryBuilder.INTERACTION_INTERACTS_WITH, "source", "target", InteractType.INTERACTS_WITH);

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
