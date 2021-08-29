package org.eol.globi.domain;

import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;

public class NodeBacked {

    private final Node underlyingNode;

    public NodeBacked(Node node) {
        this.underlyingNode = node;
    }

    public Node getUnderlyingNode() {
        return underlyingNode;
    }


    @Override
    public int hashCode() {
        return underlyingNode.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NodeBacked &&
                underlyingNode.equals(((NodeBacked) o).getUnderlyingNode());
    }

    public Relationship createRelationshipTo(Object endNode, RelType relType) {
        return createRelationshipToNoTx((NodeBacked) endNode, relType);
    }

    protected Relationship createRelationshipToNoTx(NodeBacked endNode, RelType relType) {
        Relationship rel = null;
        if (getNodeID() != endNode.getNodeID()) {
            if (!alreadyRelated(endNode, relType)) {
                rel = getUnderlyingNode().createRelationshipTo(endNode.getUnderlyingNode(), NodeUtil.asNeo4j(relType));
            }
        }
        return rel;
    }

    private boolean alreadyRelated(NodeBacked endNode, RelType relType) {
        Iterable<Relationship> relationships = getRelatedRelations(endNode, relType);

        boolean alreadyHasRelationship = false;
        Iterator<Relationship> iterator = relationships.iterator();
        while (iterator.hasNext() && !alreadyHasRelationship) {
            Relationship relationship = iterator.next();
            alreadyHasRelationship =
                    relationship.getStartNode().getId() == this.getUnderlyingNode().getId()
                            && relationship.getEndNode().getId() == endNode.getUnderlyingNode().getId();
        }
        return alreadyHasRelationship;
    }

    Iterable<Relationship> getRelatedRelations(NodeBacked endNode, RelType relType) {
        boolean thisMoreConnected = this.getUnderlyingNode().getDegree() > endNode.getUnderlyingNode().getDegree();
        Node start = thisMoreConnected ? endNode.getUnderlyingNode() : this.getUnderlyingNode();
        Direction direction = thisMoreConnected ? Direction.INCOMING : Direction.OUTGOING;
        return start.getRelationships(direction, NodeUtil.asNeo4j(relType));
    }

    public long getNodeID() {
        return getUnderlyingNode().getId();
    }

    public void setProperty(String propertyName, Object propertyValue) {
        if (propertyName != null && propertyValue != null) {
            getUnderlyingNode().setProperty(propertyName, propertyValue);
        }
    }

    protected String getPropertyValueOrNull(String propertyName) {
        Object value = getUnderlyingNode().hasProperty(propertyName)
                ? getUnderlyingNode().getProperty(propertyName)
                : null;
        return value == null ? null : value.toString();
    }

    protected String getPropertyStringValueOrNull(String propertyName) {
        Node node = getUnderlyingNode();
        return NodeUtil.getPropertyStringValueOrDefault(node, propertyName, null);
    }

    public void setExternalId(String externalId) {
        setProperty(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
    }

    public String getExternalId() {
        String propertyName = PropertyAndValueDictionary.EXTERNAL_ID;
        Object propertyValueOrNull = getPropertyValueOrNull(propertyName);
        return propertyValueOrNull == null ? null : (String) propertyValueOrNull;
    }

    protected void setPropertyIfNotNull(String name, String value) {
        if (value != null) {
            getUnderlyingNode().setProperty(name, value);
        }
    }

    protected void setPropertyIfNotNull(String name, Double value) {
        if (value != null) {
            getUnderlyingNode().setProperty(name, value);
        }
    }

    protected String getProperty(String propertyName) {
        Object value = null;
        if (getUnderlyingNode().hasProperty(propertyName)) {
            value = getUnderlyingNode().getProperty(propertyName);
        }
        return value == null ? "" : value.toString();

    }


}
