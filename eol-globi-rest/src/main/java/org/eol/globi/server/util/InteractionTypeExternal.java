package org.eol.globi.server.util;

import org.eol.globi.server.CypherQueryBuilder;

public enum InteractionTypeExternal {
    PREYS_ON(CypherQueryBuilder.INTERACTION_PREYS_ON, "predator", "prey"),
    PREYED_UPON_BY(CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "prey", "predator"),
    PARASITE_OF(CypherQueryBuilder.INTERACTION_PARASITE_OF, "parasite", "host"),
    HAS_PARASITE(CypherQueryBuilder.INTERACTION_HAS_PARASITE, "host", "parasite"),
    POLLINATES(CypherQueryBuilder.INTERACTION_POLLINATES, "pollinator", "plant"),
    POLLINATED_BY(CypherQueryBuilder.INTERACTION_POLLINATED_BY, "plant", "pollinator"),
    PATHOGEN_OF(CypherQueryBuilder.INTERACTION_PATHOGEN_OF, "pathogen", "host"),
    HAS_PATHOGEN(CypherQueryBuilder.INTERACTION_HAS_PATHOGEN, "host", "pathogen"),
    SYMBIONT_OF(CypherQueryBuilder.INTERACTION_SYMBIONT_OF, "source", "target"),
    INTERACTS_WITH(CypherQueryBuilder.INTERACTION_INTERACTS_WITH, "source", "target");


    private InteractionTypeExternal(String label, String source, String target) {
        this.label = label;
        this.source = source;
        this.target = target;
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

    private String label;
    private String source;
    private String target;


}
