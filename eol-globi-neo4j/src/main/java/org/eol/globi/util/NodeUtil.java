package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.api.Neo4jTypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NodeUtil {

    public static final int TRANSACTION_BATCH_SIZE_DEFAULT = 1000;

    public static String getPropertyStringValueOrDefault(Node node, String propertyName, String defaultValue) {
        Transaction tx = node.getGraphDatabase().beginTx();
        try {
            String value = node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : defaultValue;
            tx.success();
            return value;

        } finally {
            tx.close();
        }
    }

    public static String truncateTaxonName(String taxonName) {
        String truncatedName = taxonName;
        if (!StringUtils.containsIgnoreCase(taxonName, "virus") && StringUtils.isNotBlank(taxonName)) {
            String[] nameParts = StringUtils.split(taxonName);
            if (nameParts.length > 2) {
                truncatedName = nameParts[0].trim() + " " + nameParts[1].trim();
            }
        }
        return truncatedName;
    }

    public static void connectTaxa(Taxon taxon, TaxonNode taxonNode, GraphDatabaseService graphDb, RelTypes relType) {
        try (Transaction tx = graphDb.beginTx()) {
            TaxonNode sameAsTaxon = new TaxonNode(graphDb.createNode());
            TaxonUtil.copy(taxon, sameAsTaxon);
            taxonNode.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), asNeo4j(relType));
            tx.success();
        }
    }

    public static List<StudyNode> findAllStudies(GraphDatabaseService graphService) {
        final List<StudyNode> studies = new ArrayList<>();
        findStudies(graphService, study -> studies.add(study));
        return studies;
    }

    public static void findStudies(GraphDatabaseService graphService, StudyNodeListener listener) {
        Transaction transaction = graphService.beginTx();
        try {
            Index<Node> studyIndex = graphService.index().forNodes("studies");
            IndexHits<Node> hits = studyIndex.query("title", "*");
            for (Node hit : hits) {
                listener.onStudy(new StudyNode(hit));
            }
            hits.close();
            transaction.success();
        } finally {
            transaction.close();
        }
    }

    public static RelationshipType asNeo4j(RelType type) {
        return () -> type.name();
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
        Transaction tx = study.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            Iterable<Relationship> rels = study.getUnderlyingNode().getRelationships(asNeo4j(RelTypes.IN_DATASET), Direction.OUTGOING);
            Iterator<Relationship> iterator = rels.iterator();
            Node datasetNode = iterator.hasNext() ? iterator.next().getEndNode() : null;
            tx.success();
            return datasetNode;
        } finally {
            tx.close();
        }
    }

    public static Index<Node> forNodes(GraphDatabaseService graphDb, String indexName) {
        Index<Node> index;
        Transaction tx = graphDb.beginTx();
        try {
            index = graphDb.index().forNodes(indexName);
            tx.success();
        } finally {
            tx.close();
        }
        return index;
    }

    public static Index<Node> forNodes(GraphDatabaseService graphDb, String indexName, Map properties) {
        Index<Node> index;
        Transaction tx = graphDb.beginTx();
        try {
            index = graphDb.index().forNodes(indexName, properties);
            tx.success();
        } finally {
            tx.close();
        }
        return index;
    }

    public static void handleCollectedRelationships(NodeTypeDirection ntd, final RelationshipListener listener) {
        handleCollectedRelationships(ntd, listener, TRANSACTION_BATCH_SIZE_DEFAULT);
    }

    public static void handleCollectedRelationships(NodeTypeDirection ntd, final RelationshipListener listener, int batchSize) {
        int counter = 0;
        Transaction tx = ntd.srcNode.getGraphDatabase().beginTx();
        try {
            Iterable<Relationship> relIterable = getOutgoingNodeRelationships(ntd.srcNode, ntd.relType, ntd.dir);
            for (Relationship rel : relIterable) {
                listener.on(rel);
                if (++counter % batchSize == 0) {
                    tx.success();
                    tx.close();
                    tx = ntd.srcNode.getGraphDatabase().beginTx();
                }
            }
            tx.success();
        } finally {
            tx.close();
        }
    }

    public interface RelationshipListener {
        void on(Relationship relationship);
    }
}
