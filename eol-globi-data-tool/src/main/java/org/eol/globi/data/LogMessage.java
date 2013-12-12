package org.eol.globi.data;

import org.eol.globi.domain.NodeBacked;
import org.neo4j.graphdb.Node;

import java.util.logging.Level;

public class LogMessage extends NodeBacked {

    public LogMessage(Node node, String message, Level level) {
        super(node);
        node.setProperty("msg", message);
        node.setProperty("level", level.getName());
    }
}
