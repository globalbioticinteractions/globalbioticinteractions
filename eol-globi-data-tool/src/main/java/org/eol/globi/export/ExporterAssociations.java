package org.eol.globi.export;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterAssociations extends ExporterAssociationsBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship collectedRel : specimens) {
            Node specimenNode = collectedRel.getEndNode();
            if (isSpecimenClassified(specimenNode)) {
                handleSpecimen(study, writer, properties, specimenNode);
            }
        }
    }

    private void handleSpecimen(Study study, Writer writer, Map<String, String> properties, Node specimenNode) throws IOException {
        Iterable<Relationship> interactRelationships = specimenNode.getRelationships(Direction.OUTGOING, InteractType.values());
        if (interactRelationships.iterator().hasNext()) {
            for (Relationship interactRel : interactRelationships) {
                Node targetSpecimen = interactRel.getEndNode();

                if (isSpecimenClassified(targetSpecimen)) {
                    writeRow(study, writer, properties, specimenNode, interactRel, targetSpecimen);
                }
            }
        }
    }

    private void writeRow(Study study, Writer writer, Map<String, String> properties, Node specimenNode, Relationship interactRel, Node targetSpecimen) throws IOException {

        properties.put(EOLDictionary.ASSOCIATION_TYPE, getEOLTermFor(interactRel.getType().name()));
        properties.put(EOLDictionary.ASSOCIATION_ID, "globi:assoc:" + interactRel.getId());
        properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:source:" + specimenNode.getId());
        properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, "globi:occur:target:" + targetSpecimen.getId());
        addStudyInfo(study, properties);
        writeProperties(writer, properties);
        properties.clear();
    }



}
