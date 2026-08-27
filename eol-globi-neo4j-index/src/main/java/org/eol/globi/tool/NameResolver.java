package org.eol.globi.tool;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NameResolver extends BatchProcessorAbstract {
    private static final Logger LOG = LoggerFactory.getLogger(NameResolver.class);
    private final TaxonFilter taxonFilter;
    private final TaxonIndexFactory taxonIndexFactory;

    public NameResolver(GraphServiceFactory factory, TaxonIndexFactory indexFactory) {
        this(factory, new KnownBadNameFilter(), indexFactory);
    }

    public NameResolver(GraphServiceFactory factory,
                        TaxonFilter taxonFilter,
                        TaxonIndexFactory taxonIndexFactor) {
        super(factory);
        this.taxonIndexFactory = taxonIndexFactor;
        this.taxonFilter = taxonFilter;
    }

    @Override
    protected String getTotalToBeProcessedQuery() {
        return "MATCH (t:Taxon_Verbatim & Taxon_Unprocessed) RETURN COUNT(t) AS totalToBeProcessed";
    }

    @Override
    protected String getNextBatchQuery(Long batchSize) {
        return "MATCH (t:Taxon_Verbatim & Taxon_Unprocessed) <-[:ORIGINALLY_DESCRIBED_AS]- (s:Specimen) RETURN elementid(s) as specimenNodeId, elementid(t) as taxonNodeId " +
                "LIMIT " + batchSize;
    }

    @Override
    protected boolean handleResultRow(Map<String, Object> next, Transaction tx) {
        boolean handled = false;
        Object taxonNodeId = next.get("taxonNodeId");
        if (taxonNodeId != null) {
            handled = true;
            Node describedAsTaxonNode = tx.getNodeByElementId(taxonNodeId.toString());
            final TaxonNode describedAsTaxon = new TaxonNode(describedAsTaxonNode);
            try {
                if (taxonFilter.shouldInclude(describedAsTaxon)) {
                    Taxon resolvedTaxon = taxonIndexFactory.create(tx).getOrCreateTaxon(describedAsTaxon);
                    if (resolvedTaxon != null) {
                        Node specimenNodeId = tx.getNodeByElementId(next.get("specimenNodeId").toString());
                        new SpecimenNode(specimenNodeId).classifyAs(resolvedTaxon);
                    }
                }
            } catch (NodeFactoryException e) {
                LOG.warn("failed to create taxon with name [" + describedAsTaxon.getName() + "] and id [" + describedAsTaxon.getExternalId() + "]", e);
            } finally {
                describedAsTaxonNode.removeLabel(NodeLabel.Taxon_Unprocessed);
            }
        }
        return handled;
    }


}
