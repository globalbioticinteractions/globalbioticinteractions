package org.trophic.graph.data;

public abstract class BaseStudyImporter implements StudyImporter {
    protected NodeFactory nodeFactory;
    protected ParserFactory parserFactory;

    public BaseStudyImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }
}
