package org.eol.globi.tool;

import org.eol.globi.data.Neo4jIndexType;
import org.eol.globi.db.GraphServiceFactoryProxy;

public class IndexInteractionsNeo4j3Test extends IndexInteractionsNeo4j2Test {

    @Override
    protected Neo4jIndexType getSchemaType() {
        return Neo4jIndexType.schema;
    }

    @Override
    protected IndexerNeo4j getInteractionIndexer() {
        return new IndexInteractionsNeo4j3(new GraphServiceFactoryProxy(getGraphDb()));
    }

}