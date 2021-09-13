package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.TransactionPerBatch;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class NodeUtil {

    public static final int TRANSACTION_BATCH_SIZE_DEFAULT = 1000;

    public static String getPropertyStringValueOrDefault(Node node, String propertyName, String defaultValue) {
        return node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : defaultValue;
    }

    public static String truncateTaxonName(String taxonName) {
        String truncatedName = taxonName;
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/672
        if (!StringUtils.containsIgnoreCase(taxonName, "virus")
                && !StringUtils.endsWith(taxonName, "V")
                && StringUtils.isNotBlank(taxonName)) {
            String[] nameParts = StringUtils.split(taxonName);
            if (nameParts.length > 2) {
                truncatedName = nameParts[0].trim() + " " + nameParts[1].trim();
            }
        }
        return truncatedName;
    }

    public static void connectTaxa(Taxon taxon, TaxonNode taxonNode, GraphDatabaseService graphDb, RelTypes relType) {
        TaxonNode sameAsTaxon = new TaxonNode(graphDb.createNode());
        TaxonUtil.copy(taxon, sameAsTaxon);
        taxonNode.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), asNeo4j(relType));
    }

    public static List<StudyNode> findAllStudies(GraphDatabaseService graphService) {
        final List<StudyNode> studies = new ArrayList<>();
        findStudies(graphService, study -> studies.add(new StudyNode(study)));
        return studies;
    }

    public static void findStudies(GraphDatabaseService graphService, NodeListener listener) {
        findStudies(graphService, listener, "title", "*");
    }

    public static void findStudies(GraphDatabaseService graphService, NodeListener listener, String queryKey, String queryValue) {
        processNodes(
                1000L,
                graphService,
                listener,
                queryKey,
                queryValue,
                "studies",
                new TransactionPerBatch(graphService)
        );
    }

    public static void findDatasetsByQuery(GraphDatabaseService graphService, DatasetNodeListener listener, String queryKey, String queryValue) {
        new NodeProcessorImpl(
                graphService,
                1000L,
                queryKey,
                queryValue,
                "datasets")
                .process(
                        node -> listener.on(new DatasetNode(node)),
                        new TransactionPerBatch(graphService)
                );
    }

    public static RelationshipType asNeo4j(RelType type) {
        return type::name;
    }

    public static RelationshipType[] asNeo4j() {
        return asNeo4j(InteractType.values());
    }

    public static RelationshipType[] asNeo4j(RelType[] values) {
        RelationshipType[] types = new RelationshipType[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = asNeo4j(values[i]);
        }
        return types;
    }

    public static Iterable<Relationship> getSpecimens(StudyNode study) {
        return getSpecimens(study, RelTypes.COLLECTED);
    }

    public static Iterable<Relationship> getSpecimensSupportedAndRefutedBy(Study study) {
        Node underlyingNode = ((NodeBacked) study).getUnderlyingNode();
        return underlyingNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(new RelType[]{RelTypes.COLLECTED, RelTypes.SUPPORTS, RelTypes.REFUTES}));
    }

    public static Iterable<Relationship> getSpecimens(StudyNode study, RelTypes relType) {
        Node underlyingNode = study.getUnderlyingNode();
        return getOutgoingNodeRelationships(underlyingNode, relType);
    }

    public static Iterable<Relationship> getSpecimens(Node studyNode) {
        return getOutgoingNodeRelationships(studyNode, RelTypes.COLLECTED);
    }

    public static Iterable<Relationship> getOutgoingNodeRelationships(Node node, RelType relType) {
        return getOutgoingNodeRelationships(node, relType, Direction.OUTGOING);
    }

    public static Iterable<Relationship> getOutgoingNodeRelationships(Node node, RelType relType, Direction dir) {
        return node.getRelationships(dir, asNeo4j(relType));
    }

    public static Iterable<Relationship> getClassifications(Specimen specimen) {
        return ((NodeBacked) specimen).getUnderlyingNode().getRelationships(Direction.OUTGOING, asNeo4j(RelTypes.CLASSIFIED_AS));
    }

    public static Iterable<Relationship> getStomachContents(Specimen specimen) {
        return ((NodeBacked) specimen).getUnderlyingNode().getRelationships(asNeo4j(InteractType.ATE), Direction.OUTGOING);
    }

    public static Iterable<Relationship> getSpecimenCaughtHere(Location location) {
        return ((NodeBacked) location).getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.INCOMING);

    }

    public static Node getDataSetForStudy(StudyNode study) {
        Iterable<Relationship> rels = study.getUnderlyingNode().getRelationships(asNeo4j(RelTypes.IN_DATASET), Direction.OUTGOING);
        Iterator<Relationship> iterator = rels.iterator();
        return iterator.hasNext() ? iterator.next().getEndNode() : null;
    }

    public static Index<Node> forNodes(GraphDatabaseService graphDb, String indexName) {
        return graphDb.index().forNodes(indexName);
    }

    public static void handleCollectedRelationships(NodeTypeDirection ntd, final RelationshipListener listener) {
        handleCollectionRelationshipsNoTx(ntd, listener);
    }

    public static void handleCollectedRelationshipsNoTx(NodeTypeDirection ntd, final RelationshipListener listener) {
        Iterable<Relationship> relIterable = getOutgoingNodeRelationships(ntd.srcNode, ntd.relType, ntd.dir);
        for (Relationship rel : relIterable) {
            listener.on(rel);
        }
    }

    private static void handleCollectionRelationshipsNoTx(NodeTypeDirection ntd, RelationshipListener listener) {
        Iterable<Relationship> relIterable = getOutgoingNodeRelationships(ntd.srcNode, ntd.relType, ntd.dir);
        for (Relationship rel : relIterable) {
            listener.on(rel);
        }
    }

    public static List<Long> getBatchOfNodes(GraphDatabaseService graphService,
                                             Long skip,
                                             Long limit,
                                             String queryKey,
                                             String queryOrQueryObject,
                                             String indexName) {
        Index<Node> index = graphService.index().forNodes(indexName);
        IndexHits<Node> studies = index.query(queryKey, queryOrQueryObject);
        List<Long> collect = studies
                .stream()
                .skip(skip)
                .limit(limit)
                .map(Node::getId)
                .collect(Collectors.toList());
        studies.close();
        return collect;
    }

    public static void processNodes(Long batchSize,
                                    GraphDatabaseService graphService,
                                    NodeListener listener,
                                    String queryKey,
                                    String queryOrQueryObject,
                                    String indexName,
                                    BatchListener batchListener) {

        new NodeProcessorImpl(graphService, batchSize, queryKey, queryOrQueryObject, indexName)
                .process(listener, batchListener);
    }

}

