package org.eol.globi.data;

import org.junit.Ignore;

@Ignore("this test fails because of ostermillerutils parses first quoted, then truncates remainder; see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/1200")
public class TableParserFactoryImplTest extends TableParserFactoryImplTestBase  {

    @Override
    public TableParserFactoryImpl getTableParserFactory() {
        return new TableParserFactoryImpl();
    }

}