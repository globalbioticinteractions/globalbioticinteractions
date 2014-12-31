package org.eol.globi.export;

import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExporterOccurrences extends ExporterOccurrencesBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship collectedRel : specimens) {
            Node specimenNode = collectedRel.getEndNode();
            if (isSpecimenClassified(specimenNode)) {
                handleSpecimen(study, writer, collectedRel, specimenNode);
            }
        }
    }

    private void handleSpecimen(Study study, Writer writer, Relationship collectedRel, Node specimenNode) throws IOException {
        Iterable<Relationship> collectedAt = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.COLLECTED_AT);
        Node locationNode = null;
        for (Relationship relationship1 : collectedAt) {
            locationNode = relationship1.getEndNode();
        }
        Map<String, String> properties = new HashMap<String, String>();
        addOccurrenceProperties(locationNode, collectedRel, properties, specimenNode, study);
        writeProperties(writer, properties);
    }

    private void addOccurrenceProperties(Node locationNode, Relationship collectedRelationship, Map<String, String> properties, Node specimenNode, Study study) throws IOException {
        if (specimenNode != null) {
            Iterable<Relationship> relationships = specimenNode.getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
            Iterator<Relationship> iterator = relationships.iterator();
            if (iterator.hasNext()) {
                Relationship classifiedAs = iterator.next();
                if (classifiedAs != null) {
                    Node taxonNode = classifiedAs.getEndNode();
                    if (taxonNode.hasProperty(PropertyAndValueDictionary.EXTERNAL_ID)) {
                        String taxonId = (String) taxonNode.getProperty(PropertyAndValueDictionary.EXTERNAL_ID);
                        if (taxonId != null) {
                            properties.put(EOLDictionary.TAXON_ID, taxonId);
                        }
                    }
                    if (taxonNode.hasProperty(PropertyAndValueDictionary.NAME)) {
                        String taxonName = (String) taxonNode.getProperty(PropertyAndValueDictionary.NAME);
                        if (taxonName != null) {
                            properties.put(EOLDictionary.SCIENTIFIC_NAME, taxonName);
                        }
                    }
                }
            }
        }

        properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:" + specimenNode.getId());
        addProperty(properties, specimenNode, Specimen.LIFE_STAGE_LABEL, EOLDictionary.LIFE_STAGE);
        addProperty(properties, specimenNode, Specimen.PHYSIOLOGICAL_STATE_LABEL, EOLDictionary.PHYSIOLOGICAL_STATE);
        addProperty(properties, specimenNode, Specimen.BODY_PART_LABEL, EOLDictionary.BODY_PART);
        addProperty(properties, locationNode, Location.LATITUDE, EOLDictionary.DECIMAL_LATITUDE);
        addProperty(properties, locationNode, Location.LONGITUDE, EOLDictionary.DECIMAL_LONGITUDE);
        if (locationNode != null && locationNode.hasProperty(Location.ALTITUDE)) {
            properties.put(EOLDictionary.VERBATIM_ELEVATION, locationNode.getProperty(Location.ALTITUDE).toString() + " m");
        }
        addProperty(properties, study.getUnderlyingNode(), Study.TITLE, EOLDictionary.EVENT_ID);

        addCollectionDate(properties, collectedRelationship, EOLDictionary.EVENT_DATE);
    }

}
