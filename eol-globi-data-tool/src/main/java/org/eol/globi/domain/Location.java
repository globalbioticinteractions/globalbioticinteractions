package org.eol.globi.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

public class Location extends NodeBacked {

    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    public static final String LATITUDE = "latitude";

    public Location(Node node) {
        super(node);
    }

    public Location(Node node, Double latitude, Double longitude, Double altitude) {
        this(node);
        if (altitude != null) {
            getUnderlyingNode().setProperty(ALTITUDE, altitude);
        }
        getUnderlyingNode().setProperty(LATITUDE, latitude);
        getUnderlyingNode().setProperty(LONGITUDE, longitude);
        getUnderlyingNode().setProperty(TYPE, Location.class.getSimpleName());
    }

    public Double getAltitude() {
        return getUnderlyingNode().hasProperty(ALTITUDE) ? (Double) getUnderlyingNode().getProperty(ALTITUDE) : null;
    }

    public Double getLongitude() {
        return (Double) getUnderlyingNode().getProperty(LONGITUDE);
    }

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