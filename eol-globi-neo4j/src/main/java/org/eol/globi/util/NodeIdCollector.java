package org.eol.globi.util;

import org.neo4j.graphdb.GraphDatabaseService;

import java.util.NavigableSet;

public interface NodeIdCollector {

    void collectIds(GraphDatabaseService graphService, String queryKey, String queryOrQueryObject, String indexName, NavigableSet<Long> ids);

}
