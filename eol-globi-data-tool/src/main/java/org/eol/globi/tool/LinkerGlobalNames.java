package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkerGlobalNames {

    private static final int BATCH_SIZE = 10;

    private static final Log LOG = LogFactory.getLog(LinkerGlobalNames.class);

    public void link(final GraphDatabaseService graphDb) throws PropertyEnricherException {
        GlobalNamesSources[] sources = GlobalNamesSources.values();
        List<GlobalNamesSources> desiredSources = Arrays.asList(sources);
        GlobalNamesService globalNamesService = new GlobalNamesService();

        Index<Node> taxons = graphDb.index().forNodes("taxons");
        IndexHits<Node> hits = taxons.query("*:*");

        final Map<Long, TaxonNode> nodeMap = new HashMap<Long, TaxonNode>();
        int counter = 1;
        for (Node hit : hits) {
            if (counter % BATCH_SIZE == 0) {
                handleBatch(graphDb, globalNamesService, nodeMap, counter, desiredSources);
            }
            TaxonNode node = new TaxonNode(hit);
            nodeMap.put(node.getNodeID(), node);
            counter++;
        }
        handleBatch(graphDb, globalNamesService, nodeMap, counter, desiredSources);
    }

    private void handleBatch(final GraphDatabaseService graphDb, GlobalNamesService globalNamesService, final Map<Long, TaxonNode> nodeMap, int counter, List<GlobalNamesSources> desiredSources) throws PropertyEnricherException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String msgPrefix = "batch #" + counter / BATCH_SIZE;
        LOG.info(msgPrefix + " preparing...");
        List<String> names = new ArrayList<String>();
        for (Map.Entry<Long, TaxonNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey() + "|" + entry.getValue().getName();
            names.add(name);
        }
        try {
            if (names.size() > 0) {
                globalNamesService.findTermsForNames(names, new TermMatchListener() {
                    @Override
                    public void foundTaxonForName(Long id, String name, Taxon taxon, boolean isExactMatch) {
                        TaxonNode taxonNode = nodeMap.get(id);
                        if (!TaxonUtil.likelyHomonym(taxon, taxonNode)) {
                            if (isExactMatch) {
                                NodeUtil.connectTaxa(taxon, taxonNode, graphDb, RelTypes.SAME_AS);
                            } else {
                                NodeUtil.connectTaxa(taxon, taxonNode, graphDb, RelTypes.SIMILAR_TO);
                            }
                        }
                    }
                }, desiredSources);
            }

        } catch (PropertyEnricherException ex) {
            LOG.error(msgPrefix + " problem matching terms: [" + StringUtils.join(names,"|") + "]", ex);
        }
        stopWatch.stop();
        LOG.info(msgPrefix + " completed in [" + stopWatch.getTime() + "] ms (" + (1.0*stopWatch.getTime() / BATCH_SIZE) + " ms/name )");

        nodeMap.clear();
    }


}
