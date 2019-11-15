package org.eol.globi.domain;

import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

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
        Relationship rel;

        try (Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx()) {
            rel = createRelationshipToNoTx((NodeBacked) endNode, relType);
            tx.success();
        }

        return rel;
    }

    protected Relationship createRelationshipToNoTx(NodeBacked endNode, RelType relType) {
        Relationship rel = null;
        if (!this.equals(endNode)) {
            Iterable<Relationship> relationships = getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(relType));
            boolean hasRelationship = false;
            Iterator<Relationship> iterator = relationships.iterator();
            while (iterator.hasNext() && !hasRelationship) {
                Relationship relationship = iterator.next();
                hasRelationship = endNode.equals(relationship.getEndNode());
            }
            if (!hasRelationship) {
                rel = getUnderlyingNode().createRelationshipTo(endNode.getUnderlyingNode(), NodeUtil.asNeo4j(relType));
            }
        }
        return rel;
    }

    public long getNodeID() {
        return getUnderlyingNode().getId();
    }

    public void setPropertyWithTx(String propertyName, Object propertyValue) {
        if (propertyName != null && propertyValue != null) {
            try (Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx()) {
                getUnderlyingNode().setProperty(propertyName, propertyValue);
                transaction.success();
            }
        }
    }

    protected Object getPropertyValueOrNull(String propertyName) {
        try (Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx()) {
            Object value = getUnderlyingNode().hasProperty(propertyName)
                    ? getUnderlyingNode().getProperty(propertyName)
                    : null;
            tx.success();
            return value;
        }
    }

    protected String getPropertyStringValueOrNull(String propertyName) {
        Node node = getUnderlyingNode();
        return NodeUtil.getPropertyStringValueOrDefault(node, propertyName, null);
    }

    public void setExternalId(String externalId) {
        setPropertyWithTx(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
    }

    public String getExternalId() {
        String propertyName = PropertyAndValueDictionary.EXTERNAL_ID;
        Object propertyValueOrNull = getPropertyValueOrNull(propertyName);
        return propertyValueOrNull == null ? null : (String) propertyValueOrNull;
    }

    protected void setProperty(String name, String value) {
        if (value != null) {
            getUnderlyingNode().setProperty(name, value);
        }
    }

    protected String getProperty(String propertyName) {
        Object value = null;
        try (Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx()) {
            if (getUnderlyingNode().hasProperty(propertyName)) {
                value = getUnderlyingNode().getProperty(propertyName);
            }
            tx.success();
        }
        return value == null ? "" : value.toString();

    }


}
