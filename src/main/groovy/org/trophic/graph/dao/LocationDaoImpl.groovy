package org.trophic.graph.dao

import com.tinkerpop.blueprints.pgm.Graph
import com.tinkerpop.gremlin.Gremlin
import org.trophic.graph.domain.Location

class LocationDaoImpl implements LocationDao {

    private Graph graph;

    static {
        Gremlin.load()
    }

    public List<Location> getLocations() {
        def results = []
        graph.v(1).outE.inV >> results
        results
    }

    public void setGraph(Graph graph){
        this.graph = graph
    }

}