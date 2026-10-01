package org.eol.globi.data;

public class TableParserFactoryProxyTest extends TableParserFactoryImplTestBase {

    @Override
    public TableParserFactory getTableParserFactory() {
        return new TableParserFactoryProxy();
    }
}