package org.eol.globi.export;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ExporterAssociations extends ExporterAssociationsBase {

    @Override
    public void doExportStudy(StudyNode study, ExportUtil.Appender writer, boolean includeHeader) throws IOException {
        Map<String, String> properties = new TreeMap<String, String>();
        AtomicReference<IOException> lastException = new AtomicReference<>();
        NodeUtil.RelationshipListener handler = collectedRel -> {
            Node specimenNode = collectedRel.getEndNode();
            if (isSpecimenClassified(specimenNode)) {
                try {
                    handleSpecimen(study, writer, properties, specimenNode);
                } catch (IOException ex) {
                    lastException.set(ex);
                }
            }

        };
        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler);
        if (lastException.get() != null) {
            throw lastException.get();
        }
    }

    private void handleSpecimen(StudyNode study, ExportUtil.Appender writer, Map<String, String> properties, Node specimenNode) throws IOException {
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

    private void writeRow(StudyNode study, ExportUtil.Appender writer, Map<String, String> properties, Node specimenNode, Relationship interactRel, Node targetSpecimen) throws IOException {
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
