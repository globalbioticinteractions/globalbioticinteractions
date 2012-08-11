package org.trophic.graph.data;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.io.Writer;

public class StudyExporterImpl implements StudyExporter {

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            writer.write("\"study\",\"predator\", \"length(mm)\",\"prey\", \"latitude\", \"longitude\", \"altitude\"");
        }
        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship relationship : specimens) {
            Node specimenNode = relationship.getEndNode();
            Iterable<Relationship> collectedAt = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.COLLECTED_AT);
            Node locationNode = null;
            for (Relationship relationship1 : collectedAt) {
                locationNode = relationship1.getEndNode();
            }

            Iterable<Relationship> prey = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.ATE);
            for (Relationship relationship1 : prey) {
                writer.write("\n");
                addRowField(writer, study.getTitle());
                addTaxonField(writer, specimenNode);
                writePropertyValueOrEmpty(writer, specimenNode, Specimen.LENGTH_IN_MM);
                Node preyNode = relationship1.getEndNode();
                addTaxonField(writer, preyNode);
                writePropertyValueOrEmpty(writer, locationNode, Location.LATITUDE);
                writePropertyValueOrEmpty(writer, locationNode, Location.LONGITUDE);
                writePropertyValueOrEmpty(writer, locationNode, Location.ALTITUDE, true);
            }

        }
    }

    private void writePropertyValueOrEmpty(Writer writer, Node node, String propertyName) throws IOException {
        writePropertyValueOrEmpty(writer, node, propertyName, false);
    }

    private void writePropertyValueOrEmpty(Writer writer, Node node, String propertyName, boolean isLast) throws IOException {
        if (node != null && node.hasProperty(propertyName)) {
            addRowField(writer, node.getProperty(propertyName), isLast);
        } else {
            addRowField(writer, null, isLast);
        }
    }


    private void addRowField(Writer writer, Object property, boolean isLast) throws IOException {
        if (null != property) {
            if (property instanceof String) {
                writer.write("\"");
            }
            writer.write(property.toString());
            if (property instanceof String) {
                writer.write("\"");
            }
        }
        if (!isLast) {
            writer.write(",");
        }
    }

    private void addTaxonField(Writer writer, Node specimenNode) throws IOException {
        String taxonString = "";
        Iterable<Relationship> relationships = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
        Relationship classifiedAs = relationships.iterator().next();
        if (classifiedAs != null) {
            Node taxonNode = classifiedAs.getEndNode();
            taxonString = (String) taxonNode.getProperty(Taxon.NAME);
        }
        addRowField(writer, taxonString);
    }

    private void addRowField(Writer writer, Object taxonString) throws IOException {
        addRowField(writer, taxonString, false);
    }
}
