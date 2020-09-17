package org.eol.globi.data;

public class DatasetImporterForBatPlant extends DatasetImporterForBatBase {

    public DatasetImporterForBatPlant(ParserFactory parserFactory, NodeFactory nodeFactory) {
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
