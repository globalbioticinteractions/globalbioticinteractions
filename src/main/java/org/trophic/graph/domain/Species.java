package org.trophic.graph.domain;

import org.neo4j.graphdb.Node;

public class Species extends Taxon {

    public Species(Node node, String name) {
        super(node, name, Species.class.getSimpleName());
    }

    public Species(Node node) {
        super(node);
    }

    public Genus getGenus() {
        return new Genus(super.isPartOf());
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getName());
    }

}
