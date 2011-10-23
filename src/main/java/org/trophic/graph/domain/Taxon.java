package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.trophic.graph.domain.RelTypes.PART_OF;

public class Taxon extends NodeBacked {
    public static final String NAME = "name";
    public static final String TYPE = "type";

    public Taxon(Node node) {
        super(node);
    }

    public Taxon(Node node, String name, String type) {
        this(node);
        setName(name);
        setType(type);
    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty(NAME);
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty(NAME, name);
    }

    public String getType() {
        return (String) getUnderlyingNode().getProperty(TYPE);
    }


    public void setType(String type) {
        getUnderlyingNode().setProperty(TYPE, type);
    }


    public Node isPartOf() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(PART_OF, Direction.OUTGOING);
        return singleRelationship == null ? null : singleRelationship.getEndNode();
    }

    public Taxon isPartOfTaxon() {
        return new Taxon(isPartOf());
    }


}
