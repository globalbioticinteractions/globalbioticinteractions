package org.eol.globi.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IndexInteractions implements Linker {
    private static final Log LOG = LogFactory.getLog(IndexInteractions.class);

    private static final RelationshipType HAS_PARTICIPANT = NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT);
    public static final RelationshipType[] INTERACTION_TYPES = NodeUtil.asNeo4j(InteractType.values());
    private final GraphDatabaseService graphDb;
    private int batchSize;

    public IndexInteractions(GraphDatabaseService graphDb) {
        this(graphDb, 20);
    }

    public IndexInteractions(GraphDatabaseService graphDb, int batchSize) {
        this.graphDb = graphDb;
        this.batchSize = batchSize;
    }

    @Override
    public void link() {
        AtomicLong specimenCount = new AtomicLong(0);
        LinkProgress progress = new LinkProgress(LOG::info, 1000);
        progress.start();
        Index<Node> datasets = graphDb.index().forNodes("datasets");
        Transaction tx = graphDb.beginTx();
        try {
            for (Node datasetNode : datasets.query("*:*")) {
                Iterable<Relationship> studyRels = datasetNode.getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.IN_DATASET));
                for (Relationship studyRel : studyRels) {
                    Node studyNode = studyRel.getStartNode();
                    Iterable<Relationship> specimens = NodeUtil.getSpecimens(studyNode);
                    for (Relationship specimen : specimens) {
                        handleSpecimen(studyNode, specimen, studyNode);
                        progress.progress();
                    }
                }
                if (specimenCount.getAndIncrement() % batchSize == 0) {
                    tx.success();
                    tx.finish();
                    tx = graphDb.beginTx();
                }
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public void handleSpecimen(Node study, Relationship specimen, Node dataset) {
        Node specimenNode = specimen.getEndNode();
        if (isNotIndexed(specimenNode)) {
            Iterable<Relationship> interactions = specimenNode.getRelationships(Direction.OUTGOING, INTERACTION_TYPES);
            Node interactionNode = graphDb.createNode();
            interactionNode.createRelationshipTo(study, NodeUtil.asNeo4j(RelTypes.DERIVED_FROM));
            interactionNode.createRelationshipTo(dataset, NodeUtil.asNeo4j(RelTypes.ACCESSED_AT));

            for (Relationship interactionRel : interactions) {
                if (!interactionRel.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                    addParticipant(interactionNode, interactionRel.getStartNode());
                    addParticipant(interactionNode, interactionRel.getEndNode());
                }
            }
        }
    }

    private static boolean isNotIndexed(Node specimenNode) {
        return !specimenNode.hasRelationship(Direction.INCOMING, HAS_PARTICIPANT);
    }

    private static void addParticipant(Node interaction, Node participant) {
        if (isNotIndexed(participant)) {
            interaction.createRelationshipTo(participant, HAS_PARTICIPANT);
        }
    }

}
