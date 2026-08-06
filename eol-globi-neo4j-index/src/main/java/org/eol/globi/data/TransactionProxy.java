package org.eol.globi.data;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.StringSearchMode;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class TransactionProxy implements Transaction {
    private final Transaction tx;
    private final AtomicBoolean shouldStartNextBatch;

    public TransactionProxy(Transaction tx, AtomicBoolean shouldStartNextBatch) {
        this.tx = tx;
        this.shouldStartNextBatch = shouldStartNextBatch;
    }

    @Override
    public Node createNode() {
        return tx.createNode();
    }

    @Override
    public Node createNode(Label... labels) {
        return tx.createNode(labels);
    }

    @Override
    public Node getNodeById(long id) {
        return tx.getNodeById(id);
    }

    @Override
    public Node getNodeByElementId(String elementId) {
        return tx.getNodeByElementId(elementId);
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return tx.getRelationshipById(id);
    }

    @Override
    public Relationship getRelationshipByElementId(String elementId) {
        return tx.getRelationshipByElementId(elementId);
    }

    @Override
    public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
        return tx.bidirectionalTraversalDescription();
    }

    @Override
    public TraversalDescription traversalDescription() {
        return tx.traversalDescription();
    }

    @Override
    public Result execute(String query) throws QueryExecutionException {
        return execute(query);
    }

    @Override
    public Result execute(String query, Map<String, Object> parameters) throws QueryExecutionException {
        return execute(query, parameters);
    }

    @Override
    public Iterable<Label> getAllLabelsInUse() {
        return tx.getAllLabelsInUse();
    }

    @Override
    public Iterable<RelationshipType> getAllRelationshipTypesInUse() {
        return tx.getAllRelationshipTypesInUse();
    }

    @Override
    public Iterable<Label> getAllLabels() {
        return tx.getAllLabels();
    }

    @Override
    public Iterable<RelationshipType> getAllRelationshipTypes() {
        return tx.getAllRelationshipTypesInUse();
    }

    @Override
    public Iterable<String> getAllPropertyKeys() {
        return tx.getAllPropertyKeys();
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key, String template, StringSearchMode searchMode) {
        return tx.findNodes(label, key, template, searchMode);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, Map<String, Object> propertyValues) {
        return tx.findNodes(label, propertyValues);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return tx.findNodes(label, key1, value1, key2, value2, key3, value3);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key1, Object value1, String key2, Object value2) {
        return tx.findNodes(label, key1, value1, key2, value2);
    }

    @Override
    public Node findNode(Label label, String key, Object value) {
        return tx.findNode(label, key, value);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key, Object value) {
        return tx.findNodes(label, key, value);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label) {
        return tx.findNodes(label);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key, String template, StringSearchMode searchMode) {
        return tx.findRelationships(relationshipType, key, template, searchMode);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, Map<String, Object> propertyValues) {
        return tx.findRelationships(relationshipType, propertyValues);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return tx.findRelationships(relationshipType, key1, value1, key2, value2, key3, value3);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key1, Object value1, String key2, Object value2) {
        return tx.findRelationships(relationshipType, key1, value1, key2, value2);
    }

    @Override
    public Relationship findRelationship(RelationshipType relationshipType, String key, Object value) {
        return tx.findRelationship(relationshipType, key, value);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key, Object value) {
        return tx.findRelationships(relationshipType, key, value);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType) {
        return tx.findRelationships(relationshipType);
    }

    @Override
    public void terminate() {
        tx.terminate();
    }

    @Override
    public ResourceIterable<Node> getAllNodes() {
        return tx.getAllNodes();
    }

    @Override
    public ResourceIterable<Relationship> getAllRelationships() {
        return tx.getAllRelationships();
    }

    @Override
    public Lock acquireWriteLock(Entity entity) {
        return tx.acquireWriteLock(entity);
    }

    @Override
    public Lock acquireReadLock(Entity entity) {
        return tx.acquireReadLock(entity);
    }

    @Override
    public Schema schema() {
        return tx.schema();
    }

    @Override
    public void commit() {
        if (shouldStartNextBatch.get()) {
            tx.commit();
        }
    }

    @Override
    public void rollback() {
        tx.rollback();
    }

    @Override
    public void close() {
        if (shouldStartNextBatch.get()) {
            tx.close();
        }
    }
}
