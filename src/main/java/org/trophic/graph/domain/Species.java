package org.trophic.graph.domain;

import org.neo4j.graphdb.Node;

public class Species extends Taxon<Species> {

    public Species(Node node, String id, String name) {
        super(node);
        setName(name);

    }
    public Species(Node node) {
        super(node);
    }

    public Genus getGenus() {
        return new Genus(super.isPartOf());
    }

    public void setGenus(Genus genus) {
        super.partOf(genus);
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getName());
    }

}
