package org.eol.globi.util;

import org.neo4j.graphdb.Relationship;

public interface RelationshipListener {
    void on(Relationship relationship);
}
