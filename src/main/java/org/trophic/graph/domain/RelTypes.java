package org.trophic.graph.domain;

import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType {
    CLASSIFIED_AS,
    IS_A,
    ATE,
    COLLECTED_AT,
    CAUGHT_DURING,
    PREYS_UPON,
    PARASITE_OF,
    HAS_HOST,
    INTERACTS_WITH,
    COLLECTED
}
