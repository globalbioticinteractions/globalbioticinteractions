package org.eol.globi.server.util;

import org.eol.globi.server.CypherQueryBuilder;

public enum InteractionTypeExternal {
    PREYS_ON(CypherQueryBuilder.INTERACTION_PREYS_ON, "predator", "prey", "http://purl.obolibrary.org/obo/RO_0002439"),
    PREYED_UPON_BY(CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "prey", "predator", "http://purl.obolibrary.org/obo/RO_0002458"),
    PARASITE_OF(CypherQueryBuilder.INTERACTION_PARASITE_OF, "parasite", "host", "http://purl.obolibrary.org/obo/RO_0002444"),
    HAS_PARASITE(CypherQueryBuilder.INTERACTION_HAS_PARASITE, "host", "parasite", "http://purl.obolibrary.org/obo/RO_0002445"),
    POLLINATES(CypherQueryBuilder.INTERACTION_POLLINATES, "pollinator", "plant", "http://purl.obolibrary.org/obo/RO_0002455"),
    POLLINATED_BY(CypherQueryBuilder.INTERACTION_POLLINATED_BY, "plant", "pollinator", "http://purl.obolibrary.org/obo/RO_0002456"),
    PATHOGEN_OF(CypherQueryBuilder.INTERACTION_PATHOGEN_OF, "pathogen", "host", "http://purl.obolibrary.org/obo/RO_0002556"),
    HAS_PATHOGEN(CypherQueryBuilder.INTERACTION_HAS_PATHOGEN, "host", "pathogen", "http://purl.obolibrary.org/obo/RO_0002557"),
    VECTOR_OF(CypherQueryBuilder.INTERACTION_VECTOR_OF, "vector", "pathogen", "http://purl.obolibrary.org/obo/RO_0002459"),
    HAS_VECTOR(CypherQueryBuilder.INTERACTION_HAS_VECTOR, "pathogen", "vector", "http://purl.obolibrary.org/obo/RO_0002460"),
    SYMBIONT_OF(CypherQueryBuilder.INTERACTION_SYMBIONT_OF, "source", "target", "http://purl.obolibrary.org/obo/RO_0002440"),
    INTERACTS_WITH(CypherQueryBuilder.INTERACTION_INTERACTS_WITH, "source", "target", "http://purl.obolibrary.org/obo/RO_0002437");


    private InteractionTypeExternal(String label, String source, String target, String termIRI) {
        this.label = label;
        this.source = source;
        this.target = target;
        this.termIRI = termIRI;
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
        return termIRI;
    }

    private String label;
    private String source;
    private String target;
    private final String termIRI;


}
