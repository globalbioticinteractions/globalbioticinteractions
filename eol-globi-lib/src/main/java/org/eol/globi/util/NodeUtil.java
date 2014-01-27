package org.eol.globi.util;

import org.neo4j.graphdb.Node;

public class NodeUtil {
    public static String getPropertyStringValueOrNull(Node node, String propertyName) {
        return node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : null;
    }
}
