package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.GlobalNamesService;
import org.eol.globi.service.GlobalNamesSources;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.service.TermMatchListener;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Linker {

    private int batchSize = 100;

    private static final Log LOG = LogFactory.getLog(Linker.class);

    public void linkToGlobalNames(final GraphDatabaseService graphDb) throws PropertyEnricherException {
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
        globalNamesService.findTermsForNames(names, new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon) {
                TaxonNode taxonNode = nodeMap.get(id);
                createSameAsTaxon(taxon, taxonNode, graphDb);
            }
        });
        stopWatch.stop();
        LOG.info(msgPrefix + " completed in [" + stopWatch.getTime() + "] ms (" + stopWatch.getTime() / counter + " ms/name )");
        nodeMap.clear();
    }

    protected void createSameAsTaxon(Taxon taxon, TaxonNode taxonNode, GraphDatabaseService graphDb) {
        Transaction tx = graphDb.beginTx();
        try {
            TaxonNode sameAsTaxon = new TaxonNode(graphDb.createNode());
            sameAsTaxon.setName(taxon.getName());
            sameAsTaxon.setPath(taxon.getPath());
            sameAsTaxon.setRank(taxon.getRank());
            sameAsTaxon.setExternalId(taxon.getExternalId());
            taxonNode.getUnderlyingNode().createRelationshipTo(sameAsTaxon.getUnderlyingNode(), RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public void linkToOpenTreeOfLife(GraphDatabaseService graphDb, OpenTreeTaxonIndex index) {
        Index<Node> taxons = graphDb.index().forNodes("taxons");
        IndexHits<Node> hits = taxons.query("*:*");
        for (Node hit : hits) {
            Iterable<Relationship> rels = hit.getRelationships(Direction.OUTGOING, RelTypes.SAME_AS);
            Map<String, Long> ottIds = new HashMap<String, Long>();
            for (Relationship rel : rels) {
                if (link(graphDb, index, ottIds, rel)) {
                    break;
                }

            }
            validate(ottIds);
        }


    }

    protected void validate(Map<String, Long> ottIds) {
        if (ottIds.size() > 1) {
            Set<Long> uniqueIds = new HashSet<Long>(ottIds.values());
            if (uniqueIds.size() > 1) {
                LOG.error("found mismatching ottIds for sameAs taxa with ids: [" + ottIds + "]");
            }

        }
    }

    protected boolean link(GraphDatabaseService graphDb, OpenTreeTaxonIndex index, Map<String, Long> ottIds, Relationship rel) {
        TaxonNode taxonNode = new TaxonNode(rel.getEndNode());
        String externalId = taxonNode.getExternalId();
        if (StringUtils.contains(externalId, TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix())) {
            ottIds.clear();
            return true;
        } else {
            externalId = StringUtils.replace(externalId, TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), "irmng:");
            externalId = StringUtils.replace(externalId, TaxonomyProvider.INDEX_FUNGORUM.getIdPrefix(), "if:");
            externalId = StringUtils.replace(externalId, TaxonomyProvider.GBIF.getIdPrefix(), "gbif:");
            externalId = StringUtils.replace(externalId, TaxonomyProvider.NCBI.getIdPrefix(), "ncbi:");
            Long ottId = index.findOpenTreeTaxonIdFor(externalId);
            if (ottId != null) {
                if (ottIds.size() == 0) {
                    Taxon taxonCopy = TaxonUtil.copy(taxonNode);
                    taxonCopy.setExternalId(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix() + ottId);
                    createSameAsTaxon(taxonCopy, new TaxonNode(rel.getStartNode()), graphDb);
                }
                ottIds.put(externalId, ottId);
            }
        }
        return false;
    }
}
