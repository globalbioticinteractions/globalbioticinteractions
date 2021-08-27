package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;

public interface GraphServiceFactory extends AutoCloseable {

    GraphDatabaseService getGraphService();

}