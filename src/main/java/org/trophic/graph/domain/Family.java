package org.trophic.graph.domain;


import org.neo4j.graphdb.Node;

public class Family extends Taxon {
    public Family(Node node, String name) {
        this(node);
        setName(name);
    }

    public Family(Node node) {
        super(node);
    }

    @Override
    public String toString() {
        return String.format("%s", getName());
    }

}
