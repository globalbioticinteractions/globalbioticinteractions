package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;

public abstract class BaseExporter implements StudyExporter {


    protected static boolean isSpecimenClassified(Node specimenNode) {
        boolean classified = false;
        Relationship classifiedAs = specimenNode.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        if (classifiedAs != null) {
            Node taxonNode = classifiedAs.getEndNode();
            if (taxonNode.hasProperty(Taxon.EXTERNAL_ID)
                    && !PropertyAndValueDictionary.NO_MATCH.equals(taxonNode.getProperty(Taxon.EXTERNAL_ID))) {
                classified = true;
            }
        }
        return classified;
    }

    protected abstract String getMetaTablePrefix();

    protected abstract String getMetaTableSuffix();

    @Override
    public void exportDarwinCoreMetaTable(Writer writer, String filename) throws IOException {
        writer.write(getMetaTablePrefix());
        writer.write(filename);
        writer.write(getMetaTableSuffix());
    }
}
