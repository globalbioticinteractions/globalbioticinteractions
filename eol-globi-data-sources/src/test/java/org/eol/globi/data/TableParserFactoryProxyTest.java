package org.eol.globi.data;

public class TableParserFactoryProxyTest extends TableParserFactoryCsvTestBase {

    @Override
    public TableParserFactory getTableParserFactory() {
        return new TableParserFactoryProxy();
    }
}