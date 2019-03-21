package org.eol.globi.util;

import org.eol.globi.domain.RelType;
import org.eol.globi.domain.RelTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

public class NodeTypeDirection {
    Node srcNode;
    Direction dir;
    RelType relType;

    public NodeTypeDirection(Node srcNode) {
        this(srcNode, RelTypes.COLLECTED);
    }

    public NodeTypeDirection(Node srcNode, RelType relType) {
        this(srcNode, relType, Direction.OUTGOING);
    }

    public NodeTypeDirection(Node srcNode, RelType collected, Direction dir) {
        this.srcNode = srcNode;
        this.relType = collected;
        this.dir = dir;
    }
}
