package org.eol.globi.tool;

import org.neo4j.graphdb.GraphDatabaseService;

public interface IndexerNeo4j {

    void index(GraphDatabaseService graphService);

}
