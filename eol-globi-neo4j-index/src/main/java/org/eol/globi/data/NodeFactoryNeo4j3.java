package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public class NodeFactoryNeo4j3 extends NodeFactoryNeo4j {

    public NodeFactoryNeo4j3(GraphDatabaseService graphDb) {
        super(graphDb);
    }






    @Override
    public LocationNode findLocationNode(Transaction tx, Location location) throws NodeFactoryException {
        validate(location);

        Node matchingLocation = null;
        if (org.eol.globi.domain.LocationUtil.hasLatLng(location)) {
            Double latitude = location.getLatitude();
            ResourceIterator<Node> nodes = tx.findNodes(NodeLabel.Location, LocationConstant.LATITUDE, latitude);
            matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
        }
        if (matchingLocation == null) {
            String locality = location.getLocality();
            if (StringUtils.isNotBlank(locality)) {
                ResourceIterator<Node> nodes = tx.findNodes(NodeLabel.Location, LocationConstant.LOCALITY, locality);
                matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
            }
        }
        if (matchingLocation == null) {
            String localityId = location.getLocalityId();
            if (StringUtils.isNotBlank(location.getLocalityId())) {
                ResourceIterator<Node> nodes = tx.findNodes(NodeLabel.Location, LocationConstant.LOCALITY_ID, localityId);
                matchingLocation = findFirstMatchingLocationIfAvailable(location, nodes);
            }
        }
        return matchingLocation == null ? null : new LocationNode(matchingLocation);
    }

    @Override
    public Location findLocation(Location location) throws NodeFactoryException {
        LocationNode locationNode;
        try (Transaction tx = getGraphDb().beginTx()) {
            locationNode = findLocationNode(tx, location);
            Location copyOf = getCopyOf(locationNode);
            tx.commit();
            return copyOf;
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

    @Override
    public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            List<EnvironmentNode> orCreateEnvironmentNodes = getOrCreateEnvironmentNodes(tx, location, externalId, name);
            List<Environment> collect = orCreateEnvironmentNodes.stream().map(nodes -> {
                EnvironmentImpl environment = new EnvironmentImpl(nodes.getExternalId());
                environment.setName(nodes.getName());
                return environment;
            }).collect(Collectors.toList());
            tx.commit();
            return collect;
        }

    }

    @Override
    public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) throws NodeFactoryException {
        List<EnvironmentNode> environmentNodes = addEnvironmentNodesToLocationNodes(getGraphDb().beginTx(), location, terms);
        return environmentNodes.stream().map(environ -> {
            EnvironmentImpl environment = new EnvironmentImpl(environ.getExternalId());
            environment.setName(environ.getName());
            return environment;
        }).collect(Collectors.toList());
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
    public Node createEnvironmentNode(Transaction tx) {
        return tx.createNode(NodeLabel.Environment);
    }



}

