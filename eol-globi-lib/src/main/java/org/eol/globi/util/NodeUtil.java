package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Node;

public class NodeUtil {
    public static String getPropertyStringValueOrNull(Node node, String propertyName) {
        return node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : null;
    }

    public static String truncateTaxonName(String taxonName) {
        String truncatedName = null;
        String[] nameParts = StringUtils.split(taxonName);
        if (nameParts.length > 2) {
            truncatedName = nameParts[0].trim() + " " + nameParts[1].trim();
        } else if (nameParts.length > 1) {
            truncatedName = nameParts[0];
        }
        return truncatedName;
    }
}
