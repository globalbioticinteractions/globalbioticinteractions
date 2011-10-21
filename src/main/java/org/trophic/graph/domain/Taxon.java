package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import static org.trophic.graph.domain.RelTypes.*;

public abstract class Taxon<T> {
    private final Node underlyingNode;

    public Taxon(Node node) {
        this.underlyingNode = node;
    }

    protected Node getUnderlyingNode() {
        return underlyingNode;
    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty("name");
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty("name", name);
    }

    @Override
    public int hashCode() {
        return underlyingNode.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Taxon &&
                underlyingNode.equals(((Taxon) o).getUnderlyingNode());
    }

    public void partOf(Taxon otherTaxon) {
        Transaction tx = underlyingNode.getGraphDatabase().beginTx();
        try {
            if (!this.equals(otherTaxon)) {
                Relationship friendRel = isPartOf(otherTaxon);
                if (friendRel == null) {
                    underlyingNode.createRelationshipTo(otherTaxon.getUnderlyingNode(), PART_OF);
                }
                tx.success();
            }
        } finally {
            tx.finish();
        }
    }

    public Node isPartOf() {
        Relationship singleRelationship = underlyingNode.getSingleRelationship(PART_OF, Direction.OUTGOING);
        return singleRelationship == null ? null : singleRelationship.getEndNode();
    }


    private Relationship isPartOf(Taxon otherTaxon) {
        Node otherNode = otherTaxon.getUnderlyingNode();
        for (Relationship rel : underlyingNode.getRelationships(Direction.INCOMING, PART_OF)) {
            if (rel.getOtherNode(underlyingNode).equals(otherNode)) {
                return rel;
            }
        }
        return null;
    }
}
