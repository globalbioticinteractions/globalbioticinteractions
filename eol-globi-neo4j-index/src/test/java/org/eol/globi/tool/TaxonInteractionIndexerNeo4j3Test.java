package org.eol.globi.tool;

import org.eol.globi.data.Neo4jIndexType;

public class TaxonInteractionIndexerNeo4j3Test extends TaxonInteractionIndexerNeo4j2Test {

    @Override
    protected Neo4jIndexType getSchemaType() {
        return Neo4jIndexType.schema;
    }


}
