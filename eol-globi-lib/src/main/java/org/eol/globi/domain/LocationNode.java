package org.eol.globi.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

public class LocationNode extends NodeBacked implements Location {

    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    public static final String LATITUDE = "latitude";
    public static final String FOOTPRINT_WKT = "footprintWKT";

    public LocationNode(Node node) {
        super(node);
    }

    public LocationNode(Node node, Location location) {
        this(node);
        if (location.getAltitude() != null) {
            getUnderlyingNode().setProperty(ALTITUDE, location.getAltitude());
        }
        if (location.getFootprintWKT() != null) {
            getUnderlyingNode().setProperty(FOOTPRINT_WKT, location.getFootprintWKT());
        }
        getUnderlyingNode().setProperty(LATITUDE, location.getLatitude());
        getUnderlyingNode().setProperty(LONGITUDE, location.getLongitude());
        getUnderlyingNode().setProperty(PropertyAndValueDictionary.TYPE, LocationNode.class.getSimpleName());
    }

    public LocationNode(Node node, Double latitude, Double longitude, Double altitude) {
        this(node, new LocationImpl(altitude, longitude, latitude, null));
    }

    public void setFootprintWKT(String footprintWKT) {
        setPropertyWithTx(FOOTPRINT_WKT, footprintWKT);
    }

    @Override
    public String getFootprintWKT() {
        return (String) getPropertyValueOrNull(FOOTPRINT_WKT);
    }

    @Override
    public Double getAltitude() {
        return getUnderlyingNode().hasProperty(ALTITUDE) ? (Double) getUnderlyingNode().getProperty(ALTITUDE) : null;
    }

    @Override
    public Double getLongitude() {
        return (Double) getUnderlyingNode().getProperty(LONGITUDE);
    }

    @Override
    public Double getLatitude() {
        return (Double) getUnderlyingNode().getProperty(LATITUDE);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" Lat: ");
        sb.append(getLatitude());
        sb.append(" Lng: ");
        sb.append(getLongitude());
        sb.append(" Alt: ");
        sb.append(getAltitude());
        return sb.toString();
    }

    public Iterable<Relationship> getSpecimenCaughtHere() {
        return getUnderlyingNode().getRelationships(RelTypes.COLLECTED_AT, Direction.INCOMING);
    }

    public void addEnvironment(Environment environment) {
        boolean needsAssociation = true;
        Iterable<Relationship> relationships = getUnderlyingNode().getRelationships(RelTypes.HAS_ENVIRONMENT, Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.getEndNode().getId() == environment.getNodeID()) {
                needsAssociation = false;
                break;
            }
        }
        if (needsAssociation) {
            createRelationshipTo(environment, RelTypes.HAS_ENVIRONMENT);
        }
    }

    public List<Environment> getEnvironments() {
        Iterable<Relationship> relationships = getUnderlyingNode().getRelationships(RelTypes.HAS_ENVIRONMENT, Direction.OUTGOING);
        List<Environment> environments = new ArrayList<Environment>();
        for (Relationship relationship : relationships) {
            environments.add(new Environment(relationship.getEndNode()));
        }
        return environments;

    }

}