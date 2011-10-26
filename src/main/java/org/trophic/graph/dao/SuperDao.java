package org.trophic.graph.dao;

import org.neo4j.graphdb.*;
import org.trophic.graph.domain.Location;

public abstract class SuperDao {

    protected Traverser getTraverserWithRelType(Node node, RelationshipType relationshipType){
        Traverser traverser = node.traverse(
                        Traverser.Order.BREADTH_FIRST,
                        StopEvaluator.END_OF_GRAPH,
                        ReturnableEvaluator.ALL_BUT_START_NODE,
                        relationshipType,
                        Direction.OUTGOING);
        return traverser;
    }

    protected Location createLocation(Node node){
        Double longitude = (Double) node.getProperty(Location.LONGITUDE);
        Double latitude = (Double) node.getProperty(Location.LATITUDE);
        Double altitude = (Double) node.getProperty(Location.ALTITUDE);
        Location location = new Location(node, longitude, latitude, altitude);
        return location;
    }

}
