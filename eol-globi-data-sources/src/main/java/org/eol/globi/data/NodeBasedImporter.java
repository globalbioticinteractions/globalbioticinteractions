package org.eol.globi.data;

public abstract class NodeBasedImporter extends BaseStudyImporter {
    protected ParserFactory parserFactory;
    private NodeFactory nodeFactory;

    NodeBasedImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        this.nodeFactory = nodeFactory;
        this.parserFactory = parserFactory;
    }

    protected NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    protected ParserFactory getParserFactory() {
        return parserFactory;
    }
}
