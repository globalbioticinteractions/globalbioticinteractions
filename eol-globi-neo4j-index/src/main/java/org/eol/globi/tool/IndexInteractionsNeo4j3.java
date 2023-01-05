package org.eol.globi.tool;

import org.eol.globi.db.GraphServiceFactory;

public class IndexInteractionsNeo4j3 extends IndexInteractionsNeo4j2 {
    public IndexInteractionsNeo4j3(GraphServiceFactory factory) {
        super(factory);
    }

    @Override
    protected String getPrefix() {
        return "";
    }

}
