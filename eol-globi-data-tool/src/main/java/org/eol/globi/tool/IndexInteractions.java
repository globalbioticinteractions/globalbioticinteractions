package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

public class IndexInteractions {

    private static final RelationshipType HAS_PARTICIPANT = NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT);


    public void link(GraphDatabaseService graphDb) throws NodeFactoryException {
        Index<Node> datasets = graphDb.index().forNodes("datasets");
        RelationshipType[] relationshipTypes = NodeUtil.asNeo4j(InteractType.values());
        for (Node node : datasets.query("*:*")) {
            DatasetNode dataset = new DatasetNode(node);
            NodeFactory factory = new NodeFactoryWithDatasetContext(new NodeFactoryNeo4j(graphDb), dataset);
            Iterable<Relationship> studyRels = dataset
                    .getUnderlyingNode()
                    .getRelationships(NodeUtil.asNeo4j(RelTypes.IN_DATASET), Direction.INCOMING);
            for (Relationship studyRel : studyRels) {
                StudyNode study = new StudyNode(studyRel.getStartNode());
                Iterable<Relationship> specimens = NodeUtil.getSpecimens(study);
                for (Relationship specimen : specimens) {
                    Iterable<Relationship> interactions = specimen.getEndNode().getRelationships(Direction.OUTGOING, relationshipTypes);
                    InteractionNode interaction = (InteractionNode) factory.createInteraction(study);
                    Transaction tx = graphDb.beginTx();
                    try {
                        for (Relationship interactionRel : interactions) {
                            if (!interactionRel.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                                addParticipant(interaction, interactionRel.getStartNode());
                                addParticipant(interaction, interactionRel.getEndNode());
                            }
                            tx.success();
                        }
                    } finally {
                        tx.finish();
                    }
                }
            }
        }
    }

    private static void addParticipant(InteractionNode interaction, Node participant) {
        if (!participant.hasRelationship(Direction.INCOMING, HAS_PARTICIPANT)) {
            interaction.getUnderlyingNode().createRelationshipTo(participant, HAS_PARTICIPANT);
        }
    }

}
