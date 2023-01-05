package org.eol.globi.domain;

import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class NodeBackedTest extends GraphDBNeo4jTestCase {

    @Test
    public void createRelationshipTo() {
        AtomicInteger relationshipCount = new AtomicInteger(0);
        long nodeId = createRelationship(1, false);
        assertRelationshipCount(relationshipCount, nodeId, 1);
    }

    @Test
    public void createPreventRedundantRelationshipTo() {
        AtomicInteger relationshipCount = new AtomicInteger(0);
        long nodeId = createRelationship(10, true);
        assertRelationshipCount(relationshipCount, nodeId, 1);
    }

    @Test
    public void createPreventRedundantRelationshipToNoCheck() {
        AtomicInteger relationshipCount = new AtomicInteger(0);
        long nodeId = createRelationship(10, false);
        assertRelationshipCount(relationshipCount, nodeId, 10);
    }

    @Test
    public void pickOptimalRelationTraversalDirection() {
        long nodeId1;
        long nodeId2 = -1;
        Node node1 = getGraphDb().createNode();
        nodeId1 = node1.getId();
        NodeBacked nodeBacked1 = new NodeBacked(node1);
        for (int i = 0; i < 10; i++) {
            Node node2 = getGraphDb().createNode();
            nodeId2 = node2.getId();
            NodeBacked nodeBacked2 = new NodeBacked(node2);
            nodeBacked1.createRelationshipTo(nodeBacked2, RelTypes.COLLECTED);
        }

        assertThat(nodeId2, Is.is(not(-1)));

        NodeBacked nodeBacked3 = new NodeBacked(getGraphDb().getNodeById(nodeId1));
        NodeBacked nodeBacked4 = new NodeBacked(getGraphDb().getNodeById(nodeId2));
        assertSingleTraverse(nodeBacked3, nodeBacked4);
    }

    @Test
    public void pickOptimalRelationTraversalDirection2() {
        long nodeId1;
        long nodeId2 = -1;
        Node node1 = getGraphDb().createNode();
        nodeId1 = node1.getId();
        NodeBacked nodeBacked1 = new NodeBacked(node1);
        Node node2 = getGraphDb().createNode();
        nodeId2 = node2.getId();
        NodeBacked nodeBacked2 = new NodeBacked(node2);
        nodeBacked1.createRelationshipTo(nodeBacked2, RelTypes.COLLECTED);

        for (int i = 0; i < 10; i++) {
            nodeBacked2.createRelationshipTo(
                    new NodeBacked(getGraphDb().createNode()),
                    RelTypes.COLLECTED);
        }

        assertThat(nodeId2, Is.is(not(-1)));

        NodeBacked nodeBacked3 = new NodeBacked(getGraphDb().getNodeById(nodeId1));
        NodeBacked nodeBacked4 = new NodeBacked(getGraphDb().getNodeById(nodeId2));
        assertSingleTraverse(nodeBacked3, nodeBacked4);
    }

    public void assertSingleTraverse(NodeBacked nodeBacked2, NodeBacked nodeBacked1) {
        Iterable<Relationship> relatedRelations = nodeBacked2.getRelatedRelations(
                nodeBacked1, RelTypes.COLLECTED);
        AtomicInteger counter = new AtomicInteger(0);
        relatedRelations.forEach(x -> counter.incrementAndGet());
        assertThat(counter.get(), Is.is(1));
    }

    @Test
    public void createPreventRelationshipWithSelf() {
        AtomicInteger relationshipCount = new AtomicInteger(0);
        long result;
        long nodeId1;
        Node node1 = getGraphDb().createNode();
        nodeId1 = node1.getId();
        NodeBacked nodeBacked1 = new NodeBacked(node1);
        nodeBacked1.createRelationshipTo(nodeBacked1, RelTypes.COLLECTED);
        result = nodeId1;
        long nodeId = result;
        Node nodeById = getGraphDb().getNodeById(nodeId);
        Iterable<Relationship> relationships = nodeById.getRelationships();
        for (Relationship ignored : relationships) {
            relationshipCount.incrementAndGet();
        }

        assertThat(relationshipCount.get(), Is.

                is(0));
    }

    public void assertRelationshipCount(AtomicInteger relationshipCount, long nodeId, int expectedRelationshipCount) {
        Node nodeById = getGraphDb().getNodeById(nodeId);
        Iterable<Relationship> relationships = nodeById.getRelationships();
        for (Relationship relationship : relationships) {
            Node endNode = relationship.getEndNode();
            assertThat(endNode.getId(), Is.is(not(nodeId)));
            assertThat(relationship.getType().name(), Is.is(RelTypes.COLLECTED.name()));
            relationshipCount.incrementAndGet();
        }

        assertThat(relationshipCount.get(), Is.is(expectedRelationshipCount));
    }

    public long createRelationship(int numberOfRedundantRelationships, boolean checkExisting) {
        long nodeId1;
        Node node1 = getGraphDb().createNode();
        nodeId1 = node1.getId();
        Node node2 = getGraphDb().createNode();
        NodeBacked nodeBacked1 = new NodeBacked(node1);
        NodeBacked nodeBacked2 = new NodeBacked(node2);
        for (int i = 0; i < numberOfRedundantRelationships; i++) {
            nodeBacked1.createRelationshipTo(nodeBacked2, RelTypes.COLLECTED, checkExisting);
        }
        return nodeId1;
    }
}
