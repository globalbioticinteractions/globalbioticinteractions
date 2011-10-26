package org.trophic.graph.dao;

import com.tinkerpop.blueprints.pgm.Graph;
import org.trophic.graph.domain.Location;

import java.util.List;

public interface LocationDao {

    List<Location> getLocations();

    void setGraph(Graph graph);

    public String sayHallo();

}