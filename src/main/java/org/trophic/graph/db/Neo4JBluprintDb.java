package org.trophic.graph.db;

import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;

public abstract class Neo4JBluprintDb {

    private static Neo4jGraph graph;

    public static Neo4jGraph getGraph(){
        if (graph == null)
            graph = new Neo4jGraph("data");
        return graph;
    }

}