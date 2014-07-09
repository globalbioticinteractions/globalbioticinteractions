package org.eol.globi.domain;

import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

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

    public Relationship createRelationshipTo(NodeBacked nodeBacked, final RelType relType) {
        RelationshipType relTypeProxy = new RelationshipType() {
            @Override
            public String name() {
                return relType.name();
            }
        };
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        Relationship rel = null;
        try {
            if (!this.equals(nodeBacked)) {
                rel = getFirstIncomingRelationshipOfType(nodeBacked, relTypeProxy);
                if (rel == null) {
                    rel = getUnderlyingNode().createRelationshipTo(nodeBacked.getUnderlyingNode(), relTypeProxy);
                }
                tx.success();
            }
        } finally {
            tx.finish();
        }
        return rel;
    }

    private Relationship getFirstIncomingRelationshipOfType(NodeBacked otherTaxon, RelationshipType relType) {
        Node otherNode = otherTaxon.getUnderlyingNode();
        for (Relationship rel : getUnderlyingNode().getRelationships(Direction.INCOMING, relType)) {
            if (rel.getOtherNode(getUnderlyingNode()).equals(otherNode)) {
                return rel;
            }
        }
        return null;
    }

    public long getNodeID() {
        return getUnderlyingNode().getId();
    }

    protected void setPropertyWithTx(String propertyName, Object propertyValue) {
        if (propertyValue != null) {
            Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
            try {
                getUnderlyingNode().setProperty(propertyName, propertyValue);
                transaction.success();
            } finally {
                transaction.finish();
            }
        }
    }

    protected Object getPropertyValueOrNull(String propertyName) {
        return getUnderlyingNode().hasProperty(propertyName) ? getUnderlyingNode().getProperty(propertyName) : null;
    }

    protected String getPropertyStringValueOrNull(String propertyName) {
        Node node = getUnderlyingNode();
        return NodeUtil.getPropertyStringValueOrNull(node, propertyName);
    }

    public void setExternalId(String externalId) {
        setPropertyWithTx(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
    }

    public String getExternalId() {
        String propertyName = PropertyAndValueDictionary.EXTERNAL_ID;
        Object propertyValueOrNull = getPropertyValueOrNull(propertyName);
        return propertyValueOrNull == null ? null : (String) propertyValueOrNull;
    }

    protected Iterable<Relationship> getRelationships(final RelType relType, Direction dir) {
        return getUnderlyingNode().getRelationships(new RelationshipType() {
            @Override
            public String name() {
                return relType.name();
            }
        }, dir);
    }

}
