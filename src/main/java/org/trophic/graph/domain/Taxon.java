package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.trophic.graph.domain.RelTypes.PART_OF;

public class Taxon extends NodeBacked {
    public Taxon(Node node) {
        super(node);

    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty("name");
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty("name", name);
    }


    public Node isPartOf() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(PART_OF, Direction.OUTGOING);
        return singleRelationship == null ? null : singleRelationship.getEndNode();
    }


}
