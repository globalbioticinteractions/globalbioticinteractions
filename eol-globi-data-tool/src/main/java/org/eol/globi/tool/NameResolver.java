package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.*;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class NameResolver {
    private static final Log LOG = LogFactory.getLog(NameResolver.class);

    private final GraphDatabaseService graphService;

    private final TaxonIndex taxonIndex;
    private final TaxonFilter taxonFilter;

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    private Long batchSize = 10000L;

    public NameResolver(GraphDatabaseService graphService, TaxonIndex index) {
        this(graphService, index, new KnownBadNameFilter());
    }
    public NameResolver(GraphDatabaseService graphService, TaxonIndex index, TaxonFilter taxonFilter) {
        this.graphService = graphService;
        this.taxonIndex = index;
        this.taxonFilter = taxonFilter;
    }

    public void resolve() {
        LOG.info("name resolving started...");
        resolveNames(batchSize);
        LOG.info("name resolving complete.");
    }

    public void resolveNames(Long batchSize) {
        StopWatch watchForEntireRun = new StopWatch();
        watchForEntireRun.start();
        StopWatch watchForBatch = new StopWatch();
        watchForBatch.start();
        Long count = 0L;

        Index<Node> studyIndex = graphService.index().forNodes("studies");
        IndexHits<Node> studies = studyIndex.query("title", "*");
        for (Node studyNode : studies) {
            final Study study1 = new StudyNode(studyNode);
            final Iterable<Relationship> specimens = NodeUtil.getSpecimens(study1);
            for (Relationship collected : specimens) {
                SpecimenNode specimen = new SpecimenNode(collected.getEndNode());
                final Relationship classifiedAs = specimen.getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
                if (classifiedAs == null) {
                    final Relationship describedAs = specimen.getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS), Direction.OUTGOING);
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
                        }
                    }
                }

            }
        }
        studies.close();
        watchForEntireRun.stop();
        LOG.info("resolved [" + count + "] names in " + getProgressMsg(count, watchForEntireRun.getTime()));
    }

    public static String getProgressMsg(Long count, long duration) {
        return String.format("[%.2f] taxon/s over [%.2f] s", (float) count * 1000.0 / duration, duration / 1000.0);
    }

}
