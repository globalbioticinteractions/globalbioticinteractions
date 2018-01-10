package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;

public abstract class DarwinCoreExporter implements StudyExporter {


    protected static boolean isSpecimenClassified(Node specimenNode) {
        boolean classified = false;
        Relationship classifiedAs = specimenNode.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
        if (classifiedAs != null) {
            Node taxonNode = classifiedAs.getEndNode();
            if (hasMatchForProperty(taxonNode, PropertyAndValueDictionary.EXTERNAL_ID)
                    && hasMatchForProperty(taxonNode, PropertyAndValueDictionary.NAME)) {
                classified = true;
            }
        }
        return classified;
    }

    private static boolean hasMatchForProperty(Node taxonNode, String propertyName) {
        return taxonNode.hasProperty(propertyName)
                && !PropertyAndValueDictionary.NO_MATCH.equals(taxonNode.getProperty(propertyName));
    }

    protected abstract String getMetaTablePrefix();

    protected abstract String getMetaTableSuffix();

    public void exportDarwinCoreMetaTable(Writer writer, String filename) throws IOException {
        writer.write(getMetaTablePrefix());
        writer.write(filename);
        writer.write(getMetaTableSuffix());
    }
}
