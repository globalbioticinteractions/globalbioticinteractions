package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

import java.util.logging.Level;

public class LogMessageImpl extends NodeBacked implements LogMessage {

    private static final String LEVEL = "level";
    private static final String MSG = "msg";

    public LogMessageImpl(Node node) {
        super(node);
    }

    public LogMessageImpl(Node node, String message, Level level) {
        super(node);
        node.setProperty(MSG, message);
        node.setProperty("level", level.getName());
    }

    @Override
    public String getMessage() {
        return getPropertyStringValueOrNull(MSG);
    }

    @Override
    public String getLevel() {
        return getPropertyStringValueOrNull(LEVEL);
    }
}
