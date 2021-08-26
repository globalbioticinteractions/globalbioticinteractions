package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.SeasonNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;

public class NodeFactoryNeo4j2 extends NodeFactoryNeo4j {

    private final Index<Node> datasets;
    private final Index<Node> studies;
    private final Index<Node> externalIds;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;


    public NodeFactoryNeo4j2(GraphDatabaseService graphDb) {
        super(graphDb);
        this.datasets = NodeUtil.forNodes(graphDb, "datasets");
        this.studies = NodeUtil.forNodes(graphDb, "studies");
        this.externalIds = NodeUtil.forNodes(graphDb, "externalIds");
        this.seasons = NodeUtil.forNodes(graphDb, "seasons");
        this.locations = NodeUtil.forNodes(graphDb, "locations");
        this.environments = NodeUtil.forNodes(graphDb, "environments");
    }

    @Override
    Node createStudyNode() {
        return getGraphDb().createNode();
    }

    @Override
    void indexStudyNode(StudyNode studyNode) {
        studies.add(studyNode.getUnderlyingNode(), StudyConstant.TITLE, studyNode.getTitle());
        studies.add(studyNode.getUnderlyingNode(), StudyConstant.TITLE_IN_NAMESPACE, getTitleInNamespace(studyNode));

    }

    @Override
    protected StudyNode findStudy(Study study) {
        String titleNS = getTitleInNamespace(study);
        final IndexHits<Node> nodes = studies.get(StudyConstant.TITLE_IN_NAMESPACE, titleNS);
        Node foundStudyNode = nodes != null ? nodes.getSingle() : null;
        return foundStudyNode == null ? null : new StudyNode(foundStudyNode);
    }

    @Override
    protected void indexDatasetNode(Dataset dataset, Node datasetNode) {
        datasets.add(datasetNode, DatasetConstant.NAMESPACE, dataset.getNamespace());
    }

    @Override
    protected Node createDatasetNode() {
        return getGraphDb().createNode();
    }

    @Override
    protected Dataset getOrCreateDatasetNoTx(Dataset originatingDataset) {
        Dataset datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {
            IndexHits<Node> datasetHits = datasets.get(DatasetConstant.NAMESPACE, originatingDataset.getNamespace());

            Node datasetNode = datasetHits.hasNext()
                    ? datasetHits.next()
                    : createDatasetNode(originatingDataset);

            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }

    @Override
    protected void indexExternalIdNode(String externalId, Node externalIdNode) {
        externalIds.add(externalIdNode, PropertyAndValueDictionary.EXTERNAL_ID, externalId);
    }

    @Override
    protected Node createExternalIdNode() {
        return getGraphDb().createNode();
    }

    @Override
    protected Node getOrCreateExternalIdNoTx(String externalId) {
        Node externalIdNode = null;
        if (StringUtils.isNotBlank(externalId)) {
            IndexHits<Node> datasetHits = externalIds.get(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
            externalIdNode = datasetHits.hasNext()
                    ? datasetHits.next()
                    : createExternalId(externalId);

        }
        return externalIdNode;
    }


    @Override
    public SeasonNode findSeason(String seasonName) {
        Node seasonHit;
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> nodeIndexHits = seasons.get(SeasonNode.TITLE, seasonName);
            seasonHit = nodeIndexHits.getSingle();
            nodeIndexHits.close();
            transaction.success();
        }
        return seasonHit == null ? null : new SeasonNode(seasonHit);
    }

    @Override
    protected void indexSeasonNode(String seasonNameLower, Node node) {
        seasons.add(node, SeasonNode.TITLE, seasonNameLower);
    }

    @Override
    protected Node createSeasonNode() {
        return getGraphDb().createNode();
    }

    @Override
    protected Node createLocationNode() {
        return getGraphDb().createNode();
    }

    @Override
    protected void indexLocation(Location location, Node node) {
        if (location.getLatitude() != null) {
            locations.add(node, LocationConstant.LATITUDE, ValueContext.numeric(location.getLatitude()));
        }
        if (location.getLongitude() != null) {
            locations.add(node, LocationConstant.LONGITUDE, ValueContext.numeric(location.getLongitude()));
        }
        if (location.getAltitude() != null) {
            locations.add(node, LocationConstant.ALTITUDE, ValueContext.numeric(location.getAltitude()));
        }
        if (StringUtils.isNotBlank(location.getFootprintWKT())) {
            locations.add(node, LocationConstant.FOOTPRINT_WKT, location.getFootprintWKT());
        }
        if (StringUtils.isNotBlank(location.getLocality())) {
            locations.add(node, LocationConstant.LOCALITY, location.getLocality());
        }
        if (StringUtils.isNotBlank(location.getLocalityId())) {
            locations.add(node, LocationConstant.LOCALITY_ID, location.getLocalityId());
        }
    }

    @Override
    public LocationNode findLocation(Location location) throws NodeFactoryException {
        Node matchingLocation = null;
        if (org.eol.globi.domain.LocationUtil.hasLatLng(location)) {
            matchingLocation = findLocationByLatitude(location);
        }
        if (matchingLocation == null) {
            matchingLocation = findLocationByLocality(location);
        }
        if (matchingLocation == null) {
            matchingLocation = findLocationByLocalityId(location);
        }
        return matchingLocation == null ? null : new LocationNode(matchingLocation);
    }


    private Node findLocationByLocality(Location location) throws NodeFactoryException {
        return location.getLocality() == null ? null : findLocationBy(location, LocationConstant.LOCALITY, location.getLocality());
    }

    private Node findLocationByLocalityId(Location location) throws NodeFactoryException {
        return location.getLocalityId() == null ? null : findLocationBy(location, LocationConstant.LOCALITY_ID, location.getLocalityId());
    }

    private Node findLocationBy(Location location, String key, String value) {
        Node matchingLocation;
        String query = key + ":\"" + QueryParser.escape(value) + "\"";
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> matchingLocations = locations.query(query);
            matchingLocation = findFirstMatchingLocationIfAvailable(location, matchingLocations);
            matchingLocations.close();
            transaction.success();
        }
        return matchingLocation;
    }


    private Node findLocationByLatitude(Location location) throws NodeFactoryException {
        Node matchingLocation;
        validate(location);
        QueryContext queryOrQueryObject = QueryContext.numericRange(LocationConstant.LATITUDE, location.getLatitude(), location.getLatitude());
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> matchingLocations = locations.query(queryOrQueryObject);
            matchingLocation = findFirstMatchingLocationIfAvailable(location, matchingLocations);
            matchingLocations.close();
            transaction.success();
        }
        return matchingLocation;
    }

    @Override
    public Node createEnvironmentNode() {
        return getGraphDb().createNode();
    }

    @Override
    public void indexEnvironmentNode(Term term, EnvironmentNode environmentNode) {
        environments.add(
                environmentNode.getUnderlyingNode(),
                PropertyAndValueDictionary.NAME,
                term.getName()
        );
    }

    @Override
    public EnvironmentNode findEnvironment(String name) {
        EnvironmentNode firstMatchingEnvironment = null;
        String query = "name:\"" + name + "\"";
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> matches = environments.query(query);
            if (matches.hasNext()) {
                firstMatchingEnvironment = new EnvironmentNode(matches.next());
            }
            matches.close();
            transaction.success();
        }
        return firstMatchingEnvironment;
    }


}

