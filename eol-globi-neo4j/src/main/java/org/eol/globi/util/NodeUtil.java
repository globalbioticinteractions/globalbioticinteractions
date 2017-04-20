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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NodeUtil {
    public static String getPropertyStringValueOrDefault(Node node, String propertyName, String defaultValue) {
        return node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : defaultValue;
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
        Transaction tx = graphDb.beginTx();
        try {
            TaxonNode sameAsTaxon = new TaxonNode(graphDb.createNode());
            TaxonUtil.copy(taxon, sameAsTaxon);
            taxonNode.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), asNeo4j(relType));
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public static List<Study> findAllStudies(GraphDatabaseService graphService) {
        final List<Study> studies = new ArrayList<Study>();
        findStudies(graphService, new StudyNodeListener() {
            public void onStudy(StudyNode study) {
                studies.add(study);
            }
        });
        return studies;
    }

    public static void findStudies(GraphDatabaseService graphService, StudyNodeListener listener) {
        Index<Node> studyIndex = graphService.index().forNodes("studies");
        IndexHits<Node> hits = studyIndex.query("title", "*");
        for (Node hit : hits) {
            listener.onStudy(new StudyNode(hit));
        }
        hits.close();
    }

    public static RelationshipType asNeo4j(RelType type) {
        return () -> type.name();
    }

    public static RelationshipType[] asNeo4j() {
        return asNeo4j(InteractType.values());
    }

    public static RelationshipType[] asNeo4j(InteractType[] values) {
        RelationshipType[] types = new RelationshipType[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = asNeo4j(values[i]);
        }
        return types;
    }

    public static Iterable<Relationship> getSpecimens(Study study) {
        Node underlyingNode = ((NodeBacked) study).getUnderlyingNode();
        return getSpecimens(underlyingNode);
    }

    public static Iterable<Relationship> getSpecimens(Node studyNode) {
        return studyNode.getRelationships(Direction.OUTGOING, asNeo4j(RelTypes.COLLECTED));
    }

    public static Iterable<Relationship> getClassifications(Specimen specimen) {
        return ((NodeBacked) specimen).getUnderlyingNode().getRelationships(Direction.OUTGOING, asNeo4j(RelTypes.CLASSIFIED_AS));
    }

    public static Iterable<Relationship> getStomachContents(Specimen specimen) {
        return ((NodeBacked) specimen).getUnderlyingNode().getRelationships(asNeo4j(InteractType.ATE), Direction.OUTGOING);
    }

    public static Iterable<Relationship> getSpecimenCaughtHere(Location location) {
        return ((NodeBacked)location).getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.INCOMING);

    }

    public static Node getDataSetForStudy(StudyNode study) {
        Iterable<Relationship> rels = study.getUnderlyingNode().getRelationships(asNeo4j(RelTypes.IN_DATASET), Direction.OUTGOING);
        Iterator<Relationship> iterator = rels.iterator();
        return iterator.hasNext() ? iterator.next().getEndNode() : null;
    }
}
