package org.trophic.graph.domain;

import org.neo4j.graphdb.Node;

public class Genus extends Taxon<Genus> {

    public Genus(Node node, String name) {
        this(node);
        setName(name);
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
        super.partOf(family);
    }

}
