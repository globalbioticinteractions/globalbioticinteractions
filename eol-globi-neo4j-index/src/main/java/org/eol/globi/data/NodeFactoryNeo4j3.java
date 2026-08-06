package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;

import java.io.File;
import java.util.Iterator;

public class NodeFactoryNeo4j3 extends NodeFactoryNeo4j {

    public NodeFactoryNeo4j3(GraphDatabaseService graphDb, File cacheDir) {
        super(graphDb, cacheDir);
    }

    public static void initSchema(GraphDatabaseService graphDb) {
        initConstraints(graphDb);
        initIndexes(graphDb);
    }

    private static void initIndexes(GraphDatabaseService graphDb) {
        createIndexIfNeeded(
                graphDb,
                NodeLabel.Location,
                LocationConstant.LATITUDE
        );
        createIndexIfNeeded(
                graphDb,
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE
        );
    }

    private static void initConstraints(GraphDatabaseService graphDb) {
        createConstraintIfNeeded(
                graphDb,
                NodeLabel.Dataset,
                DatasetConstant.NAMESPACE
        );
        createConstraintIfNeeded(
                graphDb,
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE
        );
        createConstraintIfNeeded(
                graphDb,
                NodeLabel.ExternalId,
                PropertyAndValueDictionary.EXTERNAL_ID
        );
    }

    @Override
    protected Node createSeasonNode() {
        try (Transaction transaction = getGraphDb().beginTx()) {
            Node node = transaction.createNode(NodeLabel.Season);
            transaction.commit();
            return node;
        }
    }

    @Override
    protected void indexLocation(Location location, Node node) throws NodeFactoryException {
        // should already be taken care of by constraints: do nothing
    }

    @Override
    protected Node createLocationNode(Transaction transaction1) {
        return transaction1.createNode(NodeLabel.Location);
    }

    @Override
    void indexStudyNode(StudyNode studyNode) {
        // indexing already done via constraint: do nothing
    }

    @Override
    protected Node createDatasetNode(Transaction tx) {
        return tx.createNode(NodeLabel.Dataset);
    }

    @Override
    protected void indexDatasetNode(Dataset dataset, Node datasetNode) {
        // indexing already done via constraint; do nothing
    }

    @Override
    protected void indexExternalIdNode(String externalId, Node externalIdNode) {
        // external ids already indexed through constraint, do nothing.
    }

    @Override
    protected Node createExternalIdNode(Transaction transaction) {
        return transaction.createNode(NodeLabel.ExternalId);
    }


    @Override
    public LocationNode findLocationNode(Transaction tx, Location location) throws NodeFactoryException {
        try (Transaction transaction = tx) {
            validate(location);

            Node matchingLocation = null;
            if (org.eol.globi.domain.LocationUtil.hasLatLng(location)) {
                Double latitude = location.getLatitude();
                ResourceIterator<Node> nodes = transaction.findNodes(NodeLabel.Location, LocationConstant.LATITUDE, latitude);
                matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
            }
            if (matchingLocation == null) {
                String locality = location.getLocality();
                if (StringUtils.isNotBlank(locality)) {
                    ResourceIterator<Node> nodes = transaction.findNodes(NodeLabel.Location, LocationConstant.LOCALITY, locality);
                    matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
                }
            }
            if (matchingLocation == null) {
                String localityId = location.getLocalityId();
                if (StringUtils.isNotBlank(location.getLocalityId())) {
                    ResourceIterator<Node> nodes = transaction.findNodes(NodeLabel.Location, LocationConstant.LOCALITY_ID, localityId);
                    matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
                }
            }
            transaction.commit();
            return matchingLocation == null ? null : new LocationNode(matchingLocation);
        }
    }

    @Override
    public Location findLocation(Location location) throws NodeFactoryException {
        LocationNode locationNode;
        try (Transaction tx = getGraphDb().beginTx()) {
            locationNode = findLocationNode(tx, location);
            tx.commit();
            return locationNode;
        }
    }

    @Override
    public Study getOrCreateStudy(Study study) throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            StudyNode studyNode = getOrCreateStudyNode(tx, study);
            Study copyOfStudy = copyOf(studyNode);
            tx.commit();
            return copyOfStudy;
        }


    }


    private static void createConstraintIfNeeded(GraphDatabaseService graphDb,
                                                 NodeLabel label,
                                                 String propertyName) {
        try (Transaction transaction = graphDb.beginTx()) {
            if (!transaction
                    .schema()
                    .getConstraints(label)
                    .iterator()
                    .hasNext()) {

                transaction
                        .schema()
                        .constraintFor(label)
                        .assertPropertyIsUnique(propertyName)
                        .create();
                transaction.commit();
            }
        }
    }

    private static void createIndexIfNeeded(GraphDatabaseService graphDb,
                                            NodeLabel label,
                                            String propertyName) {

        try (Transaction transaction = graphDb.beginTx()) {
            Iterable<IndexDefinition> indexes = transaction
                    .schema()
                    .getIndexes(label);

            IndexDefinition indexMatching = null;
            for (IndexDefinition index : indexes) {
                Iterator<String> keyIterator = index.getPropertyKeys().iterator();
                if (keyIterator.hasNext()) {
                    if (StringUtils.equals(keyIterator.next(), propertyName)) {
                        indexMatching = index;
                        break;
                    }
                }

            }
            if (indexMatching == null) {
                transaction
                        .schema()
                        .indexFor(NodeLabel.Location)
                        .on(propertyName)
                        .create();
            }
            transaction.commit();
        }
    }

    @Override
    protected DatasetNode getOrCreateDatasetNode(Transaction tx, Dataset originatingDataset) throws NodeFactoryException {
        DatasetNode datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {
            Node node = tx
                    .findNode(NodeLabel.Dataset,
                            DatasetConstant.NAMESPACE,
                            originatingDataset.getNamespace());

            Node datasetNode = node == null
                    ? createDatasetNode(tx, originatingDataset)
                    : node;

            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }

    @Override
    protected Node getOrCreateExternalIdNoTx(String externalId) throws NodeFactoryException {
        Node externalIdNode = null;
        if (StringUtils.isNotBlank(externalId)) {

            try (Transaction transaction = getGraphDb().beginTx()) {
                Node node = transaction.findNode(NodeLabel.ExternalId, PropertyAndValueDictionary.EXTERNAL_ID, externalId);
                externalIdNode = node == null
                        ? createExternalId(transaction, externalId)
                        : node;
                transaction.commit();
            }
        }
        return externalIdNode;
    }


    @Override
    public Node createEnvironmentNode() {
        try (Transaction transaction = getGraphDb().beginTx()) {
            Node node = transaction.createNode(NodeLabel.Environment);
            transaction.commit();
            return node;
        }
    }

}

