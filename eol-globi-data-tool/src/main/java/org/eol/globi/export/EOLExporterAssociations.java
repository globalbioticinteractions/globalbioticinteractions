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

public class EOLExporterAssociations extends EOLExporterAssociationsBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Map<String, String> properties = new HashMap<String, String>();

        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship collectedRel : specimens) {
            Node specimenNode = collectedRel.getEndNode();

            Iterable<Relationship> interactRelationships = specimenNode.getRelationships(Direction.OUTGOING, InteractType.values());
            if (interactRelationships.iterator().hasNext()) {
                for (Relationship interactRel : interactRelationships) {
                    properties.put(EOLDictionary.ASSOCIATION_ID, "globi:assoc:" + interactRel.getId());
                    properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:source:" + specimenNode.getId());
                    properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, "globi:occur:target:" + interactRel.getEndNode().getId());
                    properties.put(EOLDictionary.ASSOCIATION_TYPE, interactRel.getType().name());
                    properties.put(EOLDictionary.SOURCE, study.getTitle());
                    writeProperties(writer, properties);
                    properties.clear();
                }
            }
        }
    }

}
