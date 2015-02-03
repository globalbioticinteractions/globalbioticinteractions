package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.List;

public class NodeUtil {
    public static String getPropertyStringValueOrNull(Node node, String propertyName) {
        return node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : null;
    }

    public static String truncateTaxonName(String taxonName) {
        String truncatedName = null;
        if (StringUtils.isNotBlank(taxonName)) {
            String[] nameParts = StringUtils.split(taxonName);
            if (nameParts.length > 2) {
                truncatedName = nameParts[0].trim() + " " + nameParts[1].trim();
            } else if (nameParts.length > 1) {
                truncatedName = nameParts[0];
            }
        }
        return truncatedName;
    }

    public static void createSameAsTaxon(Taxon taxon, TaxonNode taxonNode, GraphDatabaseService graphDb) {
        Transaction tx = graphDb.beginTx();
        try {
            TaxonNode sameAsTaxon = new TaxonNode(graphDb.createNode());
            sameAsTaxon.setName(taxon.getName());
            sameAsTaxon.setPath(taxon.getPath());
            sameAsTaxon.setPathIds(taxon.getPathIds());
            sameAsTaxon.setPathNames(taxon.getPathNames());
            sameAsTaxon.setRank(taxon.getRank());
            sameAsTaxon.setExternalId(taxon.getExternalId());
            taxonNode.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public static List<Study> findAllStudies(GraphDatabaseService graphService) {
        List<Study> studies = new ArrayList<Study>();
        Index<Node> studyIndex = graphService.index().forNodes("studies");
        IndexHits<Node> hits = studyIndex.query("title", "*");
        for (Node hit : hits) {
            studies.add(new Study(hit));
        }
        return studies;
    }
}
