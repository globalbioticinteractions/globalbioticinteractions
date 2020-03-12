package org.eol.globi.data;

public class BaseImporter {
    private NodeFactory nodeFactory;

    BaseImporter(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    protected NodeFactory getNodeFactory() {
        return nodeFactory;
    }
}
