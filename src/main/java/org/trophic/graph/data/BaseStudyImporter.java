package org.trophic.graph.data;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    protected ParserFactory parserFactory;

    public BaseStudyImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(nodeFactory);
        this.parserFactory = parserFactory;
    }
}
