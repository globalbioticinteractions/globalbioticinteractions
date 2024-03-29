package org.eol.globi.domain;

import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InteractionNode extends NodeBacked implements Interaction {

    public InteractionNode(Node node) {
        super(node);
    }

    @Override
    public Collection<Specimen> getParticipants() {
        List<Specimen> participants = new ArrayList<>();
        Iterable<Relationship> rels = getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT), Direction.OUTGOING);
        for (Relationship rel : rels) {
            participants.add(new SpecimenNode(rel.getEndNode()));
        }
        return participants;
    }

    @Override
    public Study getStudy() {
        Iterable<Relationship> rels = getUnderlyingNode()
                .getRelationships(NodeUtil.asNeo4j(RelTypes.DERIVED_FROM), Direction.OUTGOING);

        return rels.iterator().hasNext()
                ? new StudyNode(rels.iterator().next().getEndNode())
                : null;
    }


}
