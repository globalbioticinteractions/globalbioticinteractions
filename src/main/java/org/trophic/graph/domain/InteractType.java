package org.trophic.graph.domain;

import org.neo4j.graphdb.RelationshipType;

public enum InteractType implements RelationshipType {
    PREYS_UPON, PARASITE_OF, HAS_HOST, INTERACTS_WITH, ATE
}
