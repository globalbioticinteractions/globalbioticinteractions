package org.eol.globi.tool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.List;

public class NameResolver {
    private static final Log LOG = LogFactory.getLog(NameResolver.class);

    private final GraphDatabaseService graphService;

    private final TaxonIndex taxonIndex;
    public static final List<String> KNOWN_BAD_NAMES = new ArrayList<String>() {
        {
            add("sp");
        }
    };

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }

    private Long batchSize = 10000L;

    public NameResolver(GraphDatabaseService graphService) {
        this(graphService, PropertyEnricherFactory.createTaxonEnricher(), new TaxonNameCorrector());
    }

    public NameResolver(GraphDatabaseService graphService, TaxonIndex index) {
        this.graphService = graphService;
        this.taxonIndex = index;
    }

    public NameResolver(GraphDatabaseService graphService, PropertyEnricher enricher, CorrectionService corrector) {
        this(graphService, new TaxonIndexImpl(enricher, corrector, graphService));
    }

    public void resolve() {
        LOG.info("name resolving started...");
        resolveNames(batchSize);
        LOG.info("name resolving complete.");
    }

    public void resolveNames(Long batchSize) {
        StopWatch watch = new StopWatch();
        watch.start();
        int count = 0;

        Index<Node> studyIndex = graphService.index().forNodes("studies");
        IndexHits<Node> studies = studyIndex.query("title", "*");
        for (Node studyNode : studies) {
            final Study study1 = new Study(studyNode);
            final Iterable<Relationship> specimens = study1.getSpecimens();
            for (Relationship collected : specimens) {
                Specimen specimen = new Specimen(collected.getEndNode());
                final Relationship classifiedAs = specimen.getUnderlyingNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
                if (classifiedAs == null) {
                    final Relationship describedAs = specimen.getUnderlyingNode().getSingleRelationship(RelTypes.ORIGINALLY_DESCRIBED_AS, Direction.OUTGOING);
                    final TaxonNode describedAsTaxon = new TaxonNode(describedAs.getEndNode());
                    try {
                        if (seeminglyGoodNameOrId(describedAsTaxon.getName(), describedAsTaxon.getExternalId())) {
                            TaxonNode resolvedTaxon = taxonIndex.getOrCreateTaxon(describedAsTaxon);
                            if (resolvedTaxon != null) {
                                specimen.classifyAs(resolvedTaxon);
                                indexTaxonInteractionIfNeeded(specimen, resolvedTaxon);
                            }
                        }
                    } catch (NodeFactoryException e) {
                        LOG.warn("failed to create taxon with name [" + describedAsTaxon.getName() + "] and id [" + describedAsTaxon.getExternalId() + "]", e);
                    } finally {
                        count++;
                        if (count % batchSize == 0) {
                            watch.stop();
                            final long duration = watch.getTime();
                            if (duration > 0) {
                                LOG.info("resolved [" + batchSize + "] names in " + getProgressMsg(batchSize, duration));
                            }
                            watch.reset();
                            watch.start();
                        }
                    }
                }

            }
        }
        studies.close();
    }

    public void indexTaxonInteractionIfNeeded(Specimen specimen, TaxonNode resolvedTaxon) {
        Transaction tx = null;
        try {
            final Iterable<Relationship> interactions = specimen.getUnderlyingNode().getRelationships(Direction.OUTGOING, InteractType.values());
            for (Relationship interaction : interactions) {
                final Relationship interactorClassifiedAs = interaction.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
                if (interactorClassifiedAs != null) {
                    final Node endNode = interactorClassifiedAs.getEndNode();
                    final Node startNode = resolvedTaxon.getUnderlyingNode();
                    final Iterable<Relationship> relationships = startNode.getRelationships(Direction.OUTGOING, InteractType.values());
                    for (Relationship relationship : relationships) {
                        if (relationship.isType(interaction.getType())
                                && (relationship.getEndNode().getId() != endNode.getId())) {
                            if (tx == null) {
                                tx = graphService.beginTx();
                            }
                            startNode.createRelationshipTo(endNode, interaction.getType());
                        }
                    }
                    if (tx != null) {
                        tx.success();
                    }
                }
            }
        } finally {
            if (tx != null) {
                tx.finish();
            }
        }
    }

    public static boolean seeminglyGoodNameOrId(String name, String externalId) {
        return externalId != null || (name != null && name.length() > 1 && !KNOWN_BAD_NAMES.contains(name));
    }

    public static String getProgressMsg(Long count, long duration) {
        return String.format("[%.2f] taxon/s over [%.2f] s", (float) count * 1000.0 / duration, duration / 1000.0);
    }
}
