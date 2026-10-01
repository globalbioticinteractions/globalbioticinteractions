package org.eol.globi.data;

public class TableParserFactoryTsvTest extends TableParserFactoryImplTestBase {

    @Override
    public TableParserFactory getTableParserFactory() {
        return new TableParserFactoryTsv();
    }

}