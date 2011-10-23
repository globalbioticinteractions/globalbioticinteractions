package org.trophic.graph.domain;

import org.neo4j.graphdb.Node;

import static org.trophic.graph.domain.RelTypes.PART_OF;

public class Genus extends Taxon {

    public Genus(Node node, String name) {
        super(node, name, Genus.class.getSimpleName());
    }

    public Genus(Node node) {
        super(node);
    }

    @Override
    public String toString() {
        return String.format("%s", getName());
    }


    public Family getFamily() {
        Node partOf = super.isPartOf();
        return partOf == null ? null : new Family(partOf);
    }

    public void setFamily(Family family) {
        super.createRelationshipTo(family, PART_OF);
    }

}
