package org.trophic.graph.data;

public class BaseImporter {
    protected NodeFactory nodeFactory;

    public BaseImporter(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }
}
