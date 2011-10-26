package org.trophic.graph.dao

import com.tinkerpop.blueprints.pgm.Graph
import com.tinkerpop.blueprints.pgm.Vertex
import com.tinkerpop.gremlin.Gremlin

class LocationRepo  {

    private Graph graph;

    static {
        Gremlin.load()
    }

    public Map<Vertex, Integer> getLocations() {
        def results = []
        // TODO cool gremlin query returnin locaitons
        results
    }

    public void setGraph(Graph graph){
        this.graph = graph
    }

    public String sayHallo(){
        "Hallo!"
    }

}