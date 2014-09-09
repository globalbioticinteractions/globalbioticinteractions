package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkerTaxonIndex {

    public void link(GraphDatabaseService graphDb) {
        Index<Node> taxons = graphDb.index().forNodes("taxons");
        Index<Node> ids = graphDb.index().forNodes("taxonExternalIds", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        IndexHits<Node> hits = taxons.query("*:*");
        for (Node hit : hits) {
            List<String> externalIds = new ArrayList<String>();
            addId(externalIds, hit);
            Iterable<Relationship> rels = hit.getRelationships(Direction.OUTGOING, RelTypes.SAME_AS);
            for (Relationship rel : rels) {
                Node endNode = rel.getEndNode();
                addId(externalIds, endNode);
            }
            Transaction tx = graphDb.beginTx();
            try {
                String aggregateIds = StringUtils.join(externalIds, CharsetConstant.SEPARATOR);
                ids.add(hit, PropertyAndValueDictionary.EXTERNAL_IDS, aggregateIds);
                hit.setProperty(PropertyAndValueDictionary.EXTERNAL_IDS, aggregateIds);
                tx.success();
            } finally {
                tx.finish();
            }
        }
        hits.close();
    }

    protected void addId(List<String> externalIds, Node endNode) {
        TaxonNode taxonNode = new TaxonNode(endNode);
        String externalId = taxonNode.getExternalId();
        if (StringUtils.isNotBlank(externalId)) {
            externalIds.add(externalId);
        }
    }
}
