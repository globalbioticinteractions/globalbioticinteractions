package org.trophic.graph.dao;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import groovy.lang.GroovyObject;
import org.trophic.graph.domain.Location;

import java.util.List;
import java.util.Map;

public class LocationDaoGroovy implements LocationDao {

    private GroovyObject locationRepo;

    @Override
    public List<Location> getLocations() {
        Object[] args = {};
        Object object = locationRepo.invokeMethod("getLocations", args);
        Map<Vertex, Integer> map = (Map<Vertex, Integer>) object;
        return null;
    }

    @Override
    public void setGraph(Graph graph) {
        Object[] args = {graph};
        locationRepo.invokeMethod("setGraph", args);
    }

    @Override
    public String sayHallo() {
        Object[] args = {};
        return (String) locationRepo.invokeMethod("sayHallo", args);
    }

    public void setLocationRepo(GroovyObject locationRepo) {
        this.locationRepo = locationRepo;
    }
}
