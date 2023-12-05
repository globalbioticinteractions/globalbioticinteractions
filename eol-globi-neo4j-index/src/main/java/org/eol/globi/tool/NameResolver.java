package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.BatchListener;
import org.eol.globi.util.NodeIdCollector;
import org.eol.globi.util.NodeListener;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class NameResolver implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(NameResolver.class);

    private final TaxonIndex taxonIndex;
    private final TaxonFilter taxonFilter;
    private final GraphServiceFactory factory;
    private final NodeIdCollector nodeIdCollector;


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
        this.nodeIdCollector = nodeIdCollector;
    }

    public void resolveNames(Long batchSize, GraphDatabaseService graphService) {
        StopWatch watchForEntireRun = new StopWatch();
        watchForEntireRun.start();
        StopWatch watchForBatch = new StopWatch();
        watchForBatch.start();
        final AtomicLong nameCount = new AtomicLong(0L);

        final TransactionPerBatch batchListener = new TransactionPerBatch(graphService);

        NodeListener listener = node -> nameCount.set(resolveNamesInStudy(batchSize,
                watchForBatch,
                nameCount.get(),
                node,
                batchListener));

        LOG.info("resolving names...");

        NodeUtil.processNodes(
                batchSize,
                graphService,
                listener,
                StudyConstant.TITLE_IN_NAMESPACE,
                "*",
                "studies",
                batchListener,
                nodeIdCollector);

        watchForEntireRun.stop();
        LOG.info("resolved [" + nameCount + "] names in " + getProgressMsg(nameCount.get(), watchForEntireRun.getTime()));

    }

    private Long resolveNamesInStudy(Long batchSize,
                                     StopWatch watchForBatch,
                                     Long nameCount,
                                     Node studyNode,
                                     BatchListener batchListener) {
        batchListener.onStart();
        final Study study1 = new StudyNode(studyNode);
        String namespace = "no/namespace";
        Dataset originatingDataset = study1.getOriginatingDataset();
        if (originatingDataset != null) {
            namespace = originatingDataset.getNamespace();
        }
        LOG.info("[" + namespace + "]: resolving names...");
        final StopWatch watchForNamespace = new StopWatch();
        watchForNamespace.start();
        final Iterable<Relationship> specimenNodes = NodeUtil.getSpecimensSupportedAndRefutedBy(study1);
        for (Relationship specimenNode : specimenNodes) {
            SpecimenNode specimen = new SpecimenNode(specimenNode.getEndNode());
            final Relationship classifiedAs = specimen.getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
            if (classifiedAs == null) {
                final Relationship describedAs = specimen.getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS), Direction.OUTGOING);
                if (describedAs == null) {
                    LOG.warn("failed to find original taxon description for specimen for [" + study1.getCitation() + "]");
                } else {
                    final TaxonNode describedAsTaxon = new TaxonNode(describedAs.getEndNode());
                    try {
                        if (taxonFilter.shouldInclude(describedAsTaxon)) {
                            Taxon resolvedTaxon = taxonIndex.getOrCreateTaxon(describedAsTaxon);
                            if (resolvedTaxon != null) {
                                specimen.classifyAs(resolvedTaxon);
                            }
                        }
                    } catch (UnlikelyTaxonNameException e) {
                        // ignore
                    } catch (NodeFactoryException e) {
                        LOG.warn("failed to create taxon with name [" + describedAsTaxon.getName() + "] and id [" + describedAsTaxon.getExternalId() + "]", e);
                    } finally {
                        nameCount++;
                        if (nameCount % batchSize == 0) {
                            batchListener.onFinish();
                            watchForBatch.stop();
                            final long duration = watchForBatch.getTime();
                            if (duration > 0) {
                                String msg = "[" + namespace + "] resolved batch of [" + batchSize + "] names in " + getProgressMsg(batchSize, duration) + " (" + nameCount + " names resolved so far)";
                                LOG.info(msg);
                            }
                            watchForBatch.reset();
                            watchForBatch.start();
                            batchListener.onStart();
                        }
                    }
                }
            }
        }
        LOG.info("[" + namespace + "]: resolved [" + nameCount + "] names in [" + watchForNamespace.getTime()/1000 + "]s");
        return nameCount;
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
