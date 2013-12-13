package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

import java.util.logging.Level;

public class LogMessage extends NodeBacked {

    private static final String LEVEL = "level";
    private static final String MSG = "msg";

    public LogMessage(Node node) {
        super(node);
    }

    public LogMessage(Node node, String message, Level level) {
        super(node);
        node.setProperty(MSG, message);
        node.setProperty("level", level.getName());
    }

    public String getMessage() {
        return getPropertyStringValueOrNull(MSG);
    }

    public String getLevel() {
        return getPropertyStringValueOrNull(LEVEL);
    }
}
