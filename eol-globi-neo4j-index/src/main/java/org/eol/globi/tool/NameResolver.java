package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class NameResolver implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(NameResolver.class);

    private final TaxonIndex taxonIndex;
    private final TaxonFilter taxonFilter;

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    private Long batchSize = 10000L;

    public NameResolver(TaxonIndex index) {
        this(index, new KnownBadNameFilter());
    }

    public NameResolver(TaxonIndex index, TaxonFilter taxonFilter) {
        this.taxonIndex = index;
        this.taxonFilter = taxonFilter;
    }

    public void resolveNames(Long batchSize, GraphDatabaseService graphService) {
        StopWatch watchForEntireRun = new StopWatch();
        watchForEntireRun.start();
        StopWatch watchForBatch = new StopWatch();
        watchForBatch.start();
        Long count = 0L;

        Index<Node> studyIndex = NodeUtil.forNodes(graphService, "studies");
        Transaction transaction = graphService.beginTx();
        try {
            IndexHits<Node> studies = studyIndex.query("title", "*");
            for (Node studyNode : studies) {
                final Study study1 = new StudyNode(studyNode);
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
                            } catch (NodeFactoryException e) {
                                LOG.warn("failed to create taxon with name [" + describedAsTaxon.getName() + "] and id [" + describedAsTaxon.getExternalId() + "]", e);
                            } finally {
                                count++;
                                if (count % batchSize == 0) {
                                    watchForBatch.stop();
                                    final long duration = watchForBatch.getTime();
                                    if (duration > 0) {
                                        LOG.info("resolved batch of [" + batchSize + "] names in " + getProgressMsg(batchSize, duration));
                                    }
                                    watchForBatch.reset();
                                    watchForBatch.start();
                                    transaction.success();
                                    transaction.close();
                                    transaction = graphService.beginTx();
                                }
                            }
                        }
                    }

                }
            }
            studies.close();
            watchForEntireRun.stop();
            transaction.success();
            LOG.info("resolved [" + count + "] names in " + getProgressMsg(count, watchForEntireRun.getTime()));
        } finally {
            transaction.close();
        }
    }

    public static String getProgressMsg(Long count, long duration) {
        return String.format("[%.2f] taxon/s over [%.2f] s", (float) count * 1000.0 / duration, duration / 1000.0);
    }

    @Override
    public void index(GraphServiceFactory graphService) {
        LOG.info("name resolving started...");
        resolveNames(batchSize, graphService.getGraphService());
        LOG.info("name resolving complete.");

    }
}
