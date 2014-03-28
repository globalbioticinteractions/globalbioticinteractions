package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.GlobalNamesService;
import org.eol.globi.service.TaxonPropertyLookupServiceException;
import org.eol.globi.service.TermMatchListener;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Linker {

    private int batchSize = 100;

    private static final Log LOG = LogFactory.getLog(Linker.class);

    public void linkTaxa(final GraphDatabaseService graphDb) throws TaxonPropertyLookupServiceException {
        GlobalNamesService globalNamesService = new GlobalNamesService();

        Index<Node> taxons = graphDb.index().forNodes("taxons");
        IndexHits<Node> hits = taxons.query("*:*");

        final Map<Long, TaxonNode> nodeMap = new HashMap<Long, TaxonNode>();
        int counter = 1;
        for (Node hit : hits) {
            if (counter % batchSize == 0) {
                handleBatch(graphDb, globalNamesService, nodeMap, counter);
            }
            TaxonNode node = new TaxonNode(hit);
            nodeMap.put(node.getNodeID(), node);
            counter++;
        }

        handleBatch(graphDb, globalNamesService, nodeMap, counter);
    }

    private void handleBatch(final GraphDatabaseService graphDb, GlobalNamesService globalNamesService, final Map<Long, TaxonNode> nodeMap, int counter) throws TaxonPropertyLookupServiceException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("batch #" + counter / batchSize + " preparing...");
        List<String> names = new ArrayList<String>();
        for (Map.Entry<Long, TaxonNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey() + "|" + entry.getValue().getName();
            names.add(name);
        }
        globalNamesService.findTermsForNames(names, new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon) {
                Transaction tx = graphDb.beginTx();
                try {
                    TaxonNode sameAsTaxon = new TaxonNode(graphDb.createNode());
                    sameAsTaxon.setName(taxon.getName());
                    sameAsTaxon.setPath(taxon.getPath());
                    sameAsTaxon.setRank(taxon.getRank());
                    sameAsTaxon.setExternalId(taxon.getExternalId());
                    TaxonNode taxonNode = nodeMap.get(id);
                    taxonNode.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), RelTypes.SAME_AS);
                    tx.success();
                } finally {
                    tx.finish();
                }
            }
        });
        LOG.info("batch #" + counter / batchSize + " completed in [" + stopWatch.getTime() + "] ms (" + stopWatch.getTime() / counter + " ms/name )");
        nodeMap.clear();
    }
}
