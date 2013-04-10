package org.eol.globi.export;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class InteractionsExporter implements StudyExporter {

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            writer.write("\"study\",\"sourceTaxonName\",\"sourceTaxonId\",\"interactType\",\"targetTaxonName\",\"targetTaxonId\",\"latitude\",\"longitude\",\"altitude\"");
            writer.write(",\"collection year\",\"collection month\",\"collection day of month\"");
        }
        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship collectedRel : specimens) {
            Node specimenNode = collectedRel.getEndNode();
            Iterable<Relationship> collectedAt = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.COLLECTED_AT);
            Node locationNode = null;
            for (Relationship relationship1 : collectedAt) {
                locationNode = relationship1.getEndNode();
            }

            Iterable<Relationship> interactRelationships = specimenNode.getRelationships(Direction.OUTGOING, InteractType.ATE, InteractType.HAS_HOST, InteractType.INTERACTS_WITH, InteractType.PARASITE_OF, InteractType.PREYS_UPON);
            if (interactRelationships.iterator().hasNext()) {
                for (Relationship interactRel : interactRelationships) {
                    exportLine(study, writer, specimenNode, locationNode, interactRel, collectedRel);
                }
            }

        }
    }

    private void exportLine(Study study, Writer writer, Node predatorNode, Node locationNode, Relationship ateRelationship, Relationship collectedRelationship) throws IOException {
        writer.write("\n");
        addRowField(writer, study.getTitle());
        addTaxonField(writer, predatorNode);
        addRowField(writer,  ateRelationship == null ? null : ateRelationship.getType().name());

        Node preyNode = ateRelationship == null ? null : ateRelationship.getEndNode();
        addTaxonField(writer, preyNode);
        writePropertyValueOrEmpty(writer, locationNode, Location.LATITUDE);
        writePropertyValueOrEmpty(writer, locationNode, Location.LONGITUDE);
        writePropertyValueOrEmpty(writer, locationNode, Location.ALTITUDE);

        writeCollectionDate(writer, collectedRelationship);
    }

    private void writeCollectionDate(Writer writer, Relationship collectedRelationship) throws IOException {
        Calendar instance = null;
        if (collectedRelationship.hasProperty(Specimen.DATE_IN_UNIX_EPOCH)) {
            Long epoch = (Long) collectedRelationship.getProperty(Specimen.DATE_IN_UNIX_EPOCH);
            Date date = new Date(epoch);
            instance = Calendar.getInstance();
            instance.setTime(date);
        }

        addRowField(writer, instance == null ? null : instance.get(Calendar.YEAR));
        addRowField(writer, instance == null ? null : instance.get(Calendar.MONTH) + 1);
        addRowField(writer, instance == null ? null : instance.get(Calendar.DAY_OF_MONTH), true);
    }

    private void writePropertyValueOrEmpty(Writer writer, PropertyContainer node, String propertyName) throws IOException {
        writePropertyValueOrEmpty(writer, node, propertyName, false);
    }

    private void writePropertyValueOrEmpty(Writer writer, PropertyContainer node, String propertyName, boolean isLast) throws IOException {
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
        String taxonString = null;
        String taxonId = null;
        if (specimenNode != null) {
            Iterable<Relationship> relationships = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
            Iterator<Relationship> iterator = relationships.iterator();
            if (iterator.hasNext()) {
                Relationship classifiedAs = iterator.next();
                if (classifiedAs != null) {
                    Node taxonNode = classifiedAs.getEndNode();
                    taxonString = (String) taxonNode.getProperty(Taxon.NAME);
                    if (taxonNode.hasProperty(Taxon.EXTERNAL_ID)) {
                        taxonId = (String) taxonNode.getProperty(Taxon.EXTERNAL_ID);
                    }

                }
            }
        }
        addRowField(writer, taxonString);
        addRowField(writer, taxonId);
    }

    private void addRowField(Writer writer, Object taxonString) throws IOException {
        addRowField(writer, taxonString, false);
    }
}
