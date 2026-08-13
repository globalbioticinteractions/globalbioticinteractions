package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeIdCollector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class NameResolver implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(NameResolver.class);
    public static final String NO_NAMESPACE = "no/namespace";

    private final TaxonIndex taxonIndex;
    private final TaxonFilter taxonFilter;
    private final GraphServiceFactory factory;


    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    private Long batchSize = 10000L;

    public NameResolver(GraphServiceFactory factory, NodeIdCollector nodeIdCollector, TaxonIndex index) {
        this(factory, index, nodeIdCollector, new KnownBadNameFilter());
    }

    public NameResolver(
            GraphServiceFactory factory,
            TaxonIndex index,
            NodeIdCollector nodeIdCollector,
            TaxonFilter taxonFilter) {
        this.taxonIndex = index;
        this.taxonFilter = taxonFilter;
        this.factory = factory;
    }

    public void resolveNames(Long batchSize, GraphDatabaseService graphService) {
        long totalToBeProcessed = 0;
        try (Transaction tx = graphService.beginTx()) {
            Result result = tx.execute("MATCH (t:Taxon_Verbatim & Taxon_Unprocessed) RETURN COUNT(t) AS totalToBeProcessed");
            if (result.hasNext()) {
                totalToBeProcessed = Long.parseLong(result.next().get("totalToBeProcessed").toString());
            }
            tx.commit();
        }

        if (totalToBeProcessed == 0) {
            LOG.info("no unprocessed verbatim taxon names: nothing to do.");
        } else {
            StopWatch watchForEntireRun = new StopWatch();
            watchForEntireRun.start();
            StopWatch watchForBatch = new StopWatch();
            watchForBatch.start();
            final AtomicLong nameCount = new AtomicLong(0L);
            while (processNextBatch(batchSize, nameCount, graphService)) {
                // ignore
                if (totalToBeProcessed < nameCount.get()) {
                    LOG.info("stop name processing: processed more names (i.e., {}) than expected (i.e., {}).", nameCount.get(), totalToBeProcessed);
                    break;
                }
                watchForBatch.stop();
                final long duration = watchForBatch.getTime();
                if (duration > 0) {
                    LOG.info("resolved batch of [{}] names in {} ({} names resolved so far or {}%)",
                            batchSize,
                            getProgressMsg(batchSize, duration),
                            nameCount.get(),
                            String.format("%.1f", 100 * nameCount.get() / (float) totalToBeProcessed));
                }
                watchForBatch.reset();
                watchForBatch.start();
            }
            watchForEntireRun.stop();
            LOG.info("resolved [" + nameCount + "] names in " + getProgressMsg(nameCount.get(), watchForEntireRun.getTime()));
        }

    }

    private boolean processNextBatch(Long batchSize,
                                     AtomicLong nameCount,
                                     GraphDatabaseService graphService) {
        long numberOfNamesProcessed = 0;
        try (Transaction tx = graphService.beginTx()) {
            Result execute = tx.execute("MATCH (t:Taxon_Verbatim & Taxon_Unprocessed) <-[:ORIGINALLY_DESCRIBED_AS]- (s:Specimen) " +
                    "RETURN elementid(s) as specimenNodeId, elementid(t) as taxonNodeId " +
                    "LIMIT " + batchSize);
            while (execute.hasNext()) {
                numberOfNamesProcessed += 1;
                Map<String, Object> next = execute.next();
                Object taxonNodeId = next.get("taxonNodeId");
                if (taxonNodeId != null) {
                    nameCount.incrementAndGet();
                    Node describedAsTaxonNode = tx.getNodeByElementId(taxonNodeId.toString());
                    final TaxonNode describedAsTaxon = new TaxonNode(describedAsTaxonNode);
                    try {
                        if (taxonFilter.shouldInclude(describedAsTaxon)) {
                            Taxon resolvedTaxon = taxonIndex.getOrCreateTaxon(describedAsTaxon);
                            if (resolvedTaxon != null) {
                                Node specimenNodeId = tx.getNodeByElementId(next.get("specimenNodeId").toString());
                                new SpecimenNode(specimenNodeId).classifyAs(resolvedTaxon);
                            }
                        }
                        describedAsTaxonNode.removeLabel(NodeLabel.Taxon_Unprocessed);
                    } catch (UnlikelyTaxonNameException e) {
                        // ignore
                    } catch (NodeFactoryException e) {
                        LOG.warn("failed to create taxon with name [" + describedAsTaxon.getName() + "] and id [" + describedAsTaxon.getExternalId() + "]", e);
                    }
                }
            }
            tx.commit();
        }
        return numberOfNamesProcessed == batchSize;
    }

    public static String getProgressMsg(Long count, long duration) {
        return String.format("[%.2f] taxon/s over [%.2f] s", (float) count * 1000.0 / duration, duration / 1000.0);
    }

    @Override
    public void index() {
        LOG.info("name resolving started...");
        resolveNames(batchSize, factory.getGraphService());
        LOG.info("name resolving complete.");

    }
}
