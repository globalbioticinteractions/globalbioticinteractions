package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
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

public class LinkerTermMatcher implements Linker {

    private static final int BATCH_SIZE = 100;

    private static final Log LOG = LogFactory.getLog(LinkerTermMatcher.class);
    private final GraphDatabaseService graphDb;
    private final TermMatcher termMatcher;

    public LinkerTermMatcher(GraphDatabaseService graphDb) {
        this(graphDb, new GlobalNamesService(Arrays.asList(GlobalNamesSources.values())));
    }

    public LinkerTermMatcher(GraphDatabaseService graphDb, TermMatcher termMatcher) {
        this.graphDb = graphDb;
        this.termMatcher = termMatcher;
    }

    @Override
    public void link() {
        Index<Node> taxons = graphDb.index().forNodes("taxons");
        IndexHits<Node> hits = taxons.query("*:*");

        final Map<Long, TaxonNode> nodeMap = new HashMap<Long, TaxonNode>();
        int counter = 1;
        for (Node hit : hits) {
            if (counter % BATCH_SIZE == 0) {
                handleBatch(graphDb, termMatcher, nodeMap, counter);
            }
            TaxonNode node = new TaxonNode(hit);
            nodeMap.put(node.getNodeID(), node);
            counter++;
        }
        handleBatch(graphDb, termMatcher, nodeMap, counter);
    }

    private void handleBatch(final GraphDatabaseService graphDb, TermMatcher termMatcher, final Map<Long, TaxonNode> nodeMap, int counter) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String msgPrefix = "batch #" + counter / BATCH_SIZE;
        LOG.info(msgPrefix + " preparing...");
        List<String> nodeIdAndNames = new ArrayList<String>();
        for (Map.Entry<Long, TaxonNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey() + "|" + entry.getValue().getName();
            nodeIdAndNames.add(name);
        }
        try {
            if (nodeIdAndNames.size() > 0) {
                termMatcher.findTermsForNames(nodeIdAndNames, new TermMatchListener() {
                    @Override
                    public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType relType) {
                        TaxonNode taxonNode = nodeMap.get(nodeId);
                        if (taxonNode != null
                                && NameType.NONE != relType
                                && !TaxonUtil.likelyHomonym(taxon, taxonNode)) {
                            NodeUtil.connectTaxa(taxon, taxonNode, graphDb, RelTypes.forType(relType));
                        }
                    }
                });
            }

        } catch (PropertyEnricherException ex) {
            LOG.error(msgPrefix + " problem matching terms", ex);
        }
        stopWatch.stop();
        LOG.info(msgPrefix + " completed in [" + stopWatch.getTime() + "] ms (" + (1.0 * stopWatch.getTime() / BATCH_SIZE) + " ms/name )");

        nodeMap.clear();
    }


}
