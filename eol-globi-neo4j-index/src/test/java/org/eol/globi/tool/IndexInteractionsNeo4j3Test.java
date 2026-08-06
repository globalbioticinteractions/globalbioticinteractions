package org.eol.globi.tool;

import org.eol.globi.db.GraphServiceFactoryProxy;

public class IndexInteractionsNeo4j3Test extends IndexInteractionsNeo4j2Test {

    @Override
    protected IndexerNeo4j getInteractionIndexer() {
        return new IndexInteractionsNeo4j3(new GraphServiceFactoryProxy(getGraphDb()));
    }

}