package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.util.concurrent.atomic.AtomicLong;

public class IndexInteractions implements Linker {
    private static final Log LOG = LogFactory.getLog(IndexInteractions.class);

    private static final RelationshipType HAS_PARTICIPANT = NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT);
    public static final RelationshipType[] INTERACTION_TYPES = NodeUtil.asNeo4j(InteractType.values());
    private final GraphDatabaseService graphDb;

    public IndexInteractions(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    @Override
    public void link() {
        LinkProgress progress = new LinkProgress(LOG::info);
        progress.start();
        Index<Node> datasets = graphDb.index().forNodes("datasets");
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
                    handleSpecimen(factory, study, specimen);
                    progress.progress();
                }
            }
        }
    }

    public void handleSpecimen(NodeFactory factory, StudyNode study, Relationship specimen) {
        Node specimenNode = specimen.getEndNode();
        if (isNotIndexed(specimenNode)) {
            Iterable<Relationship> interactions = specimenNode.getRelationships(Direction.OUTGOING, INTERACTION_TYPES);
            InteractionNode interaction;
            try {
                interaction = (InteractionNode) factory.createInteraction(study);
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
            } catch (NodeFactoryException e) {
                LOG.warn("failed to create interaction", e);
            }
        }
    }

    private static boolean isNotIndexed(Node specimenNode) {
        return !specimenNode.hasRelationship(Direction.INCOMING, HAS_PARTICIPANT);
    }

    private static void addParticipant(InteractionNode interaction, Node participant) {
        if (isNotIndexed(participant)) {
            interaction.getUnderlyingNode().createRelationshipTo(participant, HAS_PARTICIPANT);
        }
    }

}
