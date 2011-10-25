package org.trophic.graph.dao;

import com.tinkerpop.blueprints.pgm.Vertex;

import java.util.Map;

public interface LocationDao {

    public Map<Vertex, Integer> getLocations();

}