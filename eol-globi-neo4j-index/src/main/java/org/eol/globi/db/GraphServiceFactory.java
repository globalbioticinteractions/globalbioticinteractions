package org.eol.globi.db;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;

public interface GraphServiceFactory {

    GraphDatabaseService getGraphService();

    /**
     * clear existing cached graph database services if present
     */
    void clear();

}