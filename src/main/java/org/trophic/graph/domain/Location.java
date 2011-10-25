package org.trophic.graph.domain;

import org.neo4j.graphdb.Node;

public class Location extends NodeBacked {

    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    public static final String LATITUDE = "latitude";

    public Location(Node node) {
        super(node);
    }

    public Location(Node node, Double latitude, Double longitude, Double altitude) {
        this(node);
        getUnderlyingNode().setProperty(ALTITUDE, altitude);
        getUnderlyingNode().setProperty(LATITUDE, latitude);
        getUnderlyingNode().setProperty(LONGITUDE, longitude);
        getUnderlyingNode().setProperty(TYPE, Location.class.getSimpleName());
    }

    public Double getAltitude() {
        return (Double) getUnderlyingNode().getProperty(ALTITUDE);
    }

    public Double getLongitude() {
        return (Double) getUnderlyingNode().getProperty(LONGITUDE);
    }

    public Double getLatitude() {
        return (Double) getUnderlyingNode().getProperty(LATITUDE);
    }

}