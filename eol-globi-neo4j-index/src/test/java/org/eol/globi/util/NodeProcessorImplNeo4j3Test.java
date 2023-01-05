package org.eol.globi.util;

import org.eol.globi.data.Neo4jIndexType;

public class NodeProcessorImplNeo4j3Test extends NodeProcessorImplNeo4j2Test {

    @Override
    protected Neo4jIndexType getSchemaType() {
        return Neo4jIndexType.schema;
    }

}