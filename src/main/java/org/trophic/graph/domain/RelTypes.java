package org.trophic.graph.domain;

import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType {
    CLASSIFIED_AS,
    IS_A,
    COLLECTED_AT,
    CAUGHT_DURING,
    COLLECTED
}
