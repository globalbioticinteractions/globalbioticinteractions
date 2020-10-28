package org.eol.globi.export;

import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExporterOccurrences extends ExporterOccurrencesBase {

    @Override
    public void doExportStudy(StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        final List<IOException> first10Exceptions = new ArrayList<>();
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), collectedRel -> {
            Node specimenNode = collectedRel.getEndNode();
            if (isSpecimenClassified(specimenNode)) {
                try {
                    handleSpecimen(study, writer, collectedRel, specimenNode);
                } catch (IOException e) {
                    if (first10Exceptions.size() < 10) {
                        first10Exceptions.add(e);
                    }
                }
            }
        });

        if (first10Exceptions.size() > 0) {
            throw first10Exceptions.get(0);
        }
    }

    private void handleSpecimen(StudyNode study, ExportUtil.Appender writer, Relationship collectedRel, Node specimenNode) throws IOException {
        Iterable<Relationship> collectedAt = specimenNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.COLLECTED_AT));
        Node locationNode = null;
        for (Relationship relationship1 : collectedAt) {
            locationNode = relationship1.getEndNode();
        }
        Map<String, String> properties = new HashMap<String, String>();
        addOccurrenceProperties(locationNode, collectedRel, properties, specimenNode, study);
        writeProperties(writer, properties);
    }

    private void addOccurrenceProperties(Node locationNode, Relationship collectedRelationship, Map<String, String> properties, Node specimenNode, StudyNode study) throws IOException {
        if (specimenNode != null) {
            Iterable<Relationship> relationships = specimenNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
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

        addProperty(properties, specimenNode, PropertyAndValueDictionary.CATALOG_NUMBER, EOLDictionary.CATALOG_NUMBER);
        addProperty(properties, specimenNode, PropertyAndValueDictionary.COLLECTION_CODE, EOLDictionary.COLLECTION_CODE);
        addProperty(properties, specimenNode, PropertyAndValueDictionary.INSTITUTION_CODE, EOLDictionary.INSTITUTION_CODE);
        addProperty(properties, specimenNode, PropertyAndValueDictionary.OCCURRENCE_ID, EOLDictionary.OCCURRENCE_ID);

        addProperty(properties, specimenNode, SpecimenConstant.BASIS_OF_RECORD_LABEL, EOLDictionary.BASIS_OF_RECORD);
        addProperty(properties, specimenNode, SpecimenConstant.LIFE_STAGE_LABEL, EOLDictionary.LIFE_STAGE);
        addProperty(properties, specimenNode, SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL, EOLDictionary.PHYSIOLOGICAL_STATE);
        addProperty(properties, specimenNode, SpecimenConstant.BODY_PART_LABEL, EOLDictionary.BODY_PART);
        addProperty(properties, locationNode, LocationConstant.LATITUDE, EOLDictionary.DECIMAL_LATITUDE);
        addProperty(properties, locationNode, LocationConstant.LONGITUDE, EOLDictionary.DECIMAL_LONGITUDE);
        if (locationNode != null && locationNode.hasProperty(LocationConstant.ALTITUDE)) {
            properties.put(EOLDictionary.VERBATIM_ELEVATION, locationNode.getProperty(LocationConstant.ALTITUDE).toString() + " m");
        }
        addProperty(properties, ((StudyNode)study).getUnderlyingNode(), StudyConstant.TITLE, EOLDictionary.EVENT_ID);

        addCollectionDate(properties, collectedRelationship, EOLDictionary.EVENT_DATE);
    }

}
