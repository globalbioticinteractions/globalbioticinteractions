package org.trophic.graph.db;

import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;

/**
 * Created by IntelliJ IDEA.
 * User: reiz
 * Date: 10/25/11
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Neo4JGraphDb {

    private static Neo4jGraph graph;

    public static Neo4jGraph getGraph(){
        if (graph == null)
            graph = new Neo4jGraph("data");
        return graph;
    }

}
