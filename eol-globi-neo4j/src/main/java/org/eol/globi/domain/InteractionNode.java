package org.eol.globi.domain;

import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class InteractionNode extends NodeBacked implements Interaction {

    public InteractionNode(Node node) {
        super(node);
    }

    @Override
    public Collection<Specimen> getParticipants() {
        List<Specimen> participants = new ArrayList<>();
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            Iterable<Relationship> rels = getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT), Direction.OUTGOING);
            for (Relationship rel : rels) {
                participants.add(new SpecimenNode(rel.getEndNode()));
            }
            tx.success();
        } finally {
            tx.close();
        }
        return participants;
    }

    @Override
    public Study getStudy() {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            Iterable<Relationship> rels = getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.DERIVED_FROM), Direction.OUTGOING);
            Study study = rels.iterator().hasNext() ? new StudyNode(rels.iterator().next().getEndNode()) : null;
            tx.success();
            return study;
        } finally {
            tx.close();
        }
    }

    @Deprecated
    @Override
    public void appendLogMessage(String message, Level level) {
        
    }

    @Deprecated
    @Override
    public List<LogMessage> getLogMessages() {
        return Collections.emptyList();
    }
}
