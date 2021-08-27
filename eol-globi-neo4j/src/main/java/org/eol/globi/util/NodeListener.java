package org.eol.globi.util;

import org.neo4j.graphdb.Node;

public interface NodeListener {
    void on(Node study);
}
