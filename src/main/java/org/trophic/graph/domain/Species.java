package org.trophic.graph.domain;

import org.neo4j.graphdb.Node;

import static org.trophic.graph.domain.RelTypes.PART_OF;

public class Species extends Taxon {

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
        super.createRelationshipTo(genus, PART_OF);
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getName());
    }

}
