package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.GlobalNamesService;
import org.eol.globi.service.GlobalNamesSources;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TermMatchListener;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkerGlobalNames {

    private int batchSize = 100;

    private static final Log LOG = LogFactory.getLog(LinkerGlobalNames.class);

    public void link(final GraphDatabaseService graphDb) throws PropertyEnricherException {
        GlobalNamesSources[] sources = GlobalNamesSources.values();
        for (GlobalNamesSources source : sources) {
            GlobalNamesService globalNamesService = new GlobalNamesService(source);

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
    }

    private void handleBatch(final GraphDatabaseService graphDb, GlobalNamesService globalNamesService, final Map<Long, TaxonNode> nodeMap, int counter) throws PropertyEnricherException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String msgPrefix = "batch #" + counter / batchSize + " for [" + globalNamesService.getSource() + "]";
        LOG.info(msgPrefix + " preparing...");
        List<String> names = new ArrayList<String>();
        for (Map.Entry<Long, TaxonNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey() + "|" + entry.getValue().getName();
            names.add(name);
        }
        if (names.size() > 0) {
            globalNamesService.findTermsForNames(names, new TermMatchListener() {
                @Override
                public void foundTaxonForName(Long id, String name, Taxon taxon) {
                    TaxonNode taxonNode = nodeMap.get(id);
                    NodeUtil.createSameAsTaxon(taxon, taxonNode, graphDb);
                }
            });
        }
        stopWatch.stop();
        LOG.info(msgPrefix + " completed in [" + stopWatch.getTime() + "] ms (" + stopWatch.getTime() / counter + " ms/name )");
        nodeMap.clear();
    }


}
