package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

public abstract class NamedNode extends NodeBacked {
    public static final String NAME = "name";

    public NamedNode(Node node) {
        super(node);
    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty(Taxon.NAME);
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty(Taxon.NAME, name);
    }
}
