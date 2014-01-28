package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

public abstract class NamedNode extends NodeBacked {

    public NamedNode(Node node) {
        super(node);
    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty(PropertyAndValueDictionary.NAME);
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty(PropertyAndValueDictionary.NAME, name);
    }
}
