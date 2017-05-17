package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

public class LocationNode extends NodeBacked implements Location {

    public LocationNode(Node node) {
        super(node);
    }

    public LocationNode(Node node, Location location) {
        this(node);
        if (location.getAltitude() != null) {
            getUnderlyingNode().setProperty(LocationConstant.ALTITUDE, location.getAltitude());
        }
        if (location.getFootprintWKT() != null) {
            getUnderlyingNode().setProperty(LocationConstant.FOOTPRINT_WKT, location.getFootprintWKT());
        }
        if (location.getLongitude() != null) {
            getUnderlyingNode().setProperty(LocationConstant.LATITUDE, location.getLatitude());
        }
        if (location.getLatitude() != null) {
            getUnderlyingNode().setProperty(LocationConstant.LONGITUDE, location.getLongitude());
        }

        getUnderlyingNode().setProperty(PropertyAndValueDictionary.TYPE, LocationNode.class.getSimpleName());
        if (StringUtils.isNotBlank(location.getLocality())) {
            getUnderlyingNode().setProperty(LocationConstant.LOCALITY, location.getLocality());
        }
        if (StringUtils.isNotBlank(location.getLocalityId())) {
            getUnderlyingNode().setProperty(LocationConstant.LOCALITY_ID, location.getLocalityId());
        }
    }

    @Override
    public String getFootprintWKT() {
        return (String) getPropertyValueOrNull(LocationConstant.FOOTPRINT_WKT);
    }

    @Override
    public String getLocality() {
        return (String) getPropertyValueOrNull(LocationConstant.LOCALITY);
    }

    @Override
    public String getLocalityId() {
        return (String) getPropertyValueOrNull(LocationConstant.LOCALITY_ID);
    }

    @Override
    public Double getAltitude() {
        return getDoubleOrNull(LocationConstant.ALTITUDE);
    }

    private Double getDoubleOrNull(String altitude) {
        return getUnderlyingNode().hasProperty(altitude) ? (Double) getUnderlyingNode().getProperty(altitude) : null;
    }

    @Override
    public Double getLongitude() {
        return getDoubleOrNull(LocationConstant.LONGITUDE);
    }

    @Override
    public Double getLatitude() {
        return getDoubleOrNull(LocationConstant.LATITUDE);
    }


    public void addEnvironment(Environment environment) {
        boolean needsAssociation = true;
        Iterable<Relationship> relationships = getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.HAS_ENVIRONMENT), Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.getEndNode().getId() == ((NodeBacked) environment).getNodeID()) {
                needsAssociation = false;
                break;
            }
        }
        if (needsAssociation) {
            createRelationshipTo(environment, RelTypes.HAS_ENVIRONMENT);
        }
    }

    public List<Environment> getEnvironments() {
        Iterable<Relationship> relationships = getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.HAS_ENVIRONMENT), Direction.OUTGOING);
        List<Environment> environments = new ArrayList<Environment>();
        for (Relationship relationship : relationships) {
            environments.add(new EnvironmentNode(relationship.getEndNode()));
        }
        return environments;

    }

}