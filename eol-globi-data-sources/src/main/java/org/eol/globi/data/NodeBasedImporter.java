package org.eol.globi.data;

public abstract class NodeBasedImporter extends BaseDatasetImporter {
    protected final ParserFactory parserFactory;
    private final NodeFactory nodeFactory;

    NodeBasedImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
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
