package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;

import java.util.Iterator;

public class NodeFactoryNeo4j3 extends NodeFactoryNeo4j {

    public NodeFactoryNeo4j3(GraphDatabaseService graphDb) {
        super(graphDb);
        initConstraints();
        initIndexes();
    }

    private void initIndexes() {
        createIndexIfNeeded(getGraphDb(),
                NodeLabel.Location,
                LocationConstant.LATITUDE);
        createIndexIfNeeded(getGraphDb(),
                NodeLabel.Location,
                LocationConstant.LOCALITY);
        createIndexIfNeeded(getGraphDb(),
                NodeLabel.Location,
                LocationConstant.LOCALITY_ID);
    }

    private void initConstraints() {
        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.Dataset,
                DatasetConstant.NAMESPACE
        );
        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE
        );
        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.ExternalId,
                PropertyAndValueDictionary.EXTERNAL_ID
        );
        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.Season,
                StudyConstant.TITLE
        );

        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.Environment,
                PropertyAndValueDictionary.NAME
        );
    }

    @Override
    protected void indexSeasonNode(String seasonNameLower, Node node) {
        // already indexed by constraint: do nothing
    }

    @Override
    protected Node createSeasonNode() {
        return getGraphDb().createNode(NodeLabel.Season);
    }

    @Override
    protected void indexLocation(Location location, Node node) {
        // should already be taken care of by constraints: do nothing
    }

    @Override
    protected Node createLocationNode() {
        return getGraphDb().createNode(NodeLabel.Location);
    }

    @Override
    Node createStudyNode() {
        return getGraphDb().createNode(NodeLabel.Reference);
    }

    @Override
    void indexStudyNode(StudyNode studyNode) {
        // indexing already done via constraint: do nothing
    }

    @Override
    protected Node createDatasetNode() {
        return getGraphDb().createNode(NodeLabel.Dataset);
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
    protected Node createExternalIdNode() {
        return getGraphDb().createNode(NodeLabel.ExternalId);
    }

    @Override
    protected StudyNode findStudy(Study study) {
        Node node = getGraphDb().findNode(NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE,
                getTitleInNamespace(study));

        return node == null
                ? null
                : new StudyNode(node);
    }

    @Override
    public void indexEnvironmentNode(Term term, EnvironmentNode environmentNode) {
        // environment nodes already automatically indexed: do nothing
    }


    @Override
    public LocationNode findLocation(Location location) throws NodeFactoryException {
        validate(location);

        Node matchingLocation = null;
        try (Transaction tx = getGraphDb().beginTx()) {
            if (org.eol.globi.domain.LocationUtil.hasLatLng(location)) {
                Double latitude = location.getLatitude();
                ResourceIterator<Node> nodes = getGraphDb().findNodes(NodeLabel.Location, LocationConstant.LATITUDE, latitude);
                matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
            }
            if (matchingLocation == null) {
                String locality = location.getLocality();
                if (StringUtils.isNotBlank(locality)) {
                    ResourceIterator<Node> nodes = getGraphDb().findNodes(NodeLabel.Location, LocationConstant.LOCALITY, locality);
                    matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
                }
            }
            if (matchingLocation == null) {
                String localityId = location.getLocalityId();
                if (StringUtils.isNotBlank(location.getLocalityId())) {
                    ResourceIterator<Node> nodes = getGraphDb().findNodes(NodeLabel.Location, LocationConstant.LOCALITY_ID, localityId);
                    matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
                }
            }
            tx.success();
            return matchingLocation == null ? null : new LocationNode(matchingLocation);
        }
    }

    @Override
    public StudyNode getOrCreateStudy(Study study) {
        Node node;
        try (Transaction tx = getGraphDb().beginTx()) {
            node = getGraphDb()
                    .findNode(NodeLabel.Reference,
                            StudyConstant.TITLE_IN_NAMESPACE,
                            getTitleInNamespace(study));
            tx.success();
        }

        return node == null
                ? createStudy(study)
                : new StudyNode(node);

    }

    @Override
    public Season findSeason(String seasonName) {
        return null;
    }


    private static void createConstraintIfNeeded(GraphDatabaseService graphDb,
                                                 NodeLabel label,
                                                 String propertyName) {
        try (Transaction transaction = graphDb.beginTx()) {
            if (!graphDb
                    .schema()
                    .getConstraints(label)
                    .iterator()
                    .hasNext()) {

                graphDb
                        .schema()
                        .constraintFor(label)
                        .assertPropertyIsUnique(propertyName)
                        .create();
            }
            transaction.success();
        }
    }

    private static void createIndexIfNeeded(GraphDatabaseService graphDb,
                                            NodeLabel label,
                                            String propertyName) {
        try (Transaction transaction = graphDb.beginTx()) {

            Iterable<IndexDefinition> indexes = graphDb
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
                graphDb
                        .schema()
                        .indexFor(label)
                        .on(propertyName)
                        .create();
            }
            transaction.success();
        }
    }

    @Override
    protected Dataset getOrCreateDatasetNoTx(Dataset originatingDataset) {
        Dataset datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {

            Node node = getGraphDb()
                    .findNode(NodeLabel.Dataset,
                            DatasetConstant.NAMESPACE,
                            originatingDataset.getNamespace());

            Node datasetNode = node == null
                    ? createDatasetNode(originatingDataset)
                    : node;

            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }

    @Override
    protected Node getOrCreateExternalIdNoTx(String externalId) {
        Node externalIdNode = null;
        if (StringUtils.isNotBlank(externalId)) {
            Node node = getGraphDb().findNode(NodeLabel.ExternalId, PropertyAndValueDictionary.EXTERNAL_ID, externalId);
            externalIdNode = node == null
                    ? createExternalId(externalId)
                    : node;
        }
        return externalIdNode;
    }


    @Override
    public Node createEnvironmentNode() {
        return getGraphDb().createNode(NodeLabel.Environment);
    }

    @Override
    public EnvironmentNode findEnvironment(String name) {
        try (Transaction tx = getGraphDb().beginTx()) {
            Node node = getGraphDb().findNode(NodeLabel.Environment,
                    PropertyAndValueDictionary.NAME,
                    name
            );
            tx.success();
            return node == null
                    ? null
                    : new EnvironmentNode(node);
        }
    }


}

