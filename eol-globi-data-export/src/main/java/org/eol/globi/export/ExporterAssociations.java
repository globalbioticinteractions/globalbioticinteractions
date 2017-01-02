package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
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
        Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
        for (Relationship collectedRel : specimens) {
            Node specimenNode = collectedRel.getEndNode();
            if (isSpecimenClassified(specimenNode)) {
                handleSpecimen(study, writer, properties, specimenNode);
            }
        }
    }

    private void handleSpecimen(Study study, Writer writer, Map<String, String> properties, Node specimenNode) throws IOException {
        Iterable<Relationship> interactRelationships = specimenNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j());
        if (interactRelationships.iterator().hasNext()) {
            for (Relationship interactRel : interactRelationships) {
                if (!interactRel.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                    Node targetSpecimen = interactRel.getEndNode();

                    if (isSpecimenClassified(targetSpecimen)) {
                        writeRow(study, writer, properties, specimenNode, interactRel, targetSpecimen);
                    }
                }
            }
        }
    }

    private void writeRow(Study study, Writer writer, Map<String, String> properties, Node specimenNode, Relationship interactRel, Node targetSpecimen) throws IOException {
        properties.put(EOLDictionary.ASSOCIATION_TYPE, getEOLTermFor(interactRel.getType().name()));
        properties.put(EOLDictionary.ASSOCIATION_ID, "globi:assoc:" + interactRel.getId());
        properties.put(EOLDictionary.OCCURRENCE_ID, toOccurrenceId(specimenNode));
        properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, toOccurrenceId(targetSpecimen));
        addStudyInfo(study, properties);
        writeProperties(writer, properties);
        properties.clear();
    }

    private String toOccurrenceId(Node specimenNode) {
        return "globi:occur:" + specimenNode.getId();
    }


}
