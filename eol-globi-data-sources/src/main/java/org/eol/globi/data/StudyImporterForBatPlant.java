package org.eol.globi.data;

public class StudyImporterForBatPlant extends StudyImporterForBatBase {

    public StudyImporterForBatPlant(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    protected String getBaseURL() {
        return "https://www.batplant.org/";
    }

    @Override
    protected String getIdPrefix() {
        return "batplant:";
    }

}
