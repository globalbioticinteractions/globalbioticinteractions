package org.eol.globi.service;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GraphDatabaseServiceProxy implements GraphDatabaseService {
    private final GraphDatabaseService graphDb;

    public GraphDatabaseServiceProxy(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    @Override
    public Node createNode() {
        return getGraphDb().createNode();
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    @Override
    public Long createNodeId() {
        return getGraphDb().createNodeId();
    }

    @Override
    public Node createNode(Label... labels) {
        return getGraphDb().createNode(labels);
    }

    @Override
    public Node getNodeById(long id) {
        return getGraphDb().getNodeById(id);
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return getGraphDb().getRelationshipById(id);
    }

    @Override
    public ResourceIterable<Node> getAllNodes() {
        return getGraphDb().getAllNodes();
    }

    @Override
    public ResourceIterable<Relationship> getAllRelationships() {
        return getGraphDb().getAllRelationships();
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key, Object value) {
        return getGraphDb().findNodes(label, key, value);
    }

    @Override
    public Node findNode(Label label, String key, Object value) {
        return getGraphDb().findNode(label, key, value);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label) {
        return getGraphDb().findNodes(label);
    }

    @Override
    public ResourceIterable<Label> getAllLabelsInUse() {
        return getGraphDb().getAllLabelsInUse();
    }

    @Override
    public ResourceIterable<RelationshipType> getAllRelationshipTypesInUse() {
        return getGraphDb().getAllRelationshipTypesInUse();
    }

    @Override
    public ResourceIterable<Label> getAllLabels() {
        return getGraphDb().getAllLabels();
    }

    @Override
    public ResourceIterable<RelationshipType> getAllRelationshipTypes() {
        return getGraphDb().getAllRelationshipTypes();
    }

    @Override
    public ResourceIterable<String> getAllPropertyKeys() {
        return getGraphDb().getAllPropertyKeys();
    }

    @Override
    public boolean isAvailable(long timeout) {
        return getGraphDb().isAvailable(timeout);
    }

    @Override
    public void shutdown() {
        getGraphDb().shutdown();
    }

    @Override
    public Transaction beginTx() {
        return getGraphDb().beginTx();
    }

    @Override
    public Transaction beginTx(long timeout, TimeUnit unit) {
        return getGraphDb().beginTx(timeout, unit);
    }

    @Override
    public Result execute(String query) throws QueryExecutionException {
        return getGraphDb().execute(query);
    }

    @Override
    public Result execute(String query, long timeout, TimeUnit unit) throws QueryExecutionException {
        return getGraphDb().execute(query, timeout, unit);
    }

    @Override
    public Result execute(String query, Map<String, Object> parameters) throws QueryExecutionException {
        return getGraphDb().execute(query, parameters);
    }

    @Override
    public Result execute(String query, Map<String, Object> parameters, long timeout, TimeUnit unit) throws QueryExecutionException {
        return getGraphDb().execute(query, parameters, timeout, unit);
    }

    @Override
    public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
        return getGraphDb().registerTransactionEventHandler(handler);
    }

    @Override
    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
        return getGraphDb().unregisterTransactionEventHandler(handler);
    }

    @Override
    public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
        return getGraphDb().registerKernelEventHandler(handler);
    }

    @Override
    public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
        return getGraphDb().unregisterKernelEventHandler(handler);
    }

    @Override
    public Schema schema() {
        return getGraphDb().schema();
    }

    @Override
    public IndexManager index() {
        return getGraphDb().index();
    }

    @Override
    public TraversalDescription traversalDescription() {
        return getGraphDb().traversalDescription();
    }

    @Override
    public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
        return getGraphDb().bidirectionalTraversalDescription();
    }
}
