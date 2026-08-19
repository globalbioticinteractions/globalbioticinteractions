package org.eol.globi.tool;

import org.eol.globi.data.NodeLabel;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.util.NodeIdCollector;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TaxonInteractionIndexer extends BatchProcessorAbstract {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonInteractionIndexer.class);
    public static final String LABEL_SELECTOR = NodeLabel.Taxon.name() + " & !" + NodeLabel.Taxon_HasInteractionShortCut.name();
    private NodeIdCollector nodeIdCollector;

    TaxonInteractionIndexer(GraphServiceFactory factory) {
        super(factory);
    }

    @Override
    protected String getTotalToBeProcessedQuery() {
        return "MATCH (t:" + LABEL_SELECTOR + ") RETURN COUNT(t) AS totalToBeProcessed";
    }

    @Override
    protected String getNextBatchQuery(Long batchSize) {
        return "MATCH (t:" + LABEL_SELECTOR + ") RETURN elementid(t) as taxonNodeId " +
                "LIMIT " + batchSize;
    }

    @Override
    protected boolean handleResultRow(Map<String, Object> next, Transaction tx) {
        boolean handled = false;
        Node sourceTaxon = tx.getNodeByElementId((String) next.get("taxonNodeId"));
        if (sourceTaxon != null && !sourceTaxon.hasLabel((NodeLabel.Taxon_HasInteractionShortCut))) {
            handled = true;
            final Iterable<Relationship> classifiedAs = sourceTaxon.getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
            for (Relationship classifiedA : classifiedAs) {
                Node specimenNode = classifiedA.getStartNode();
                final Iterable<Relationship> interactions = specimenNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.values()));
                for (Relationship interaction : interactions) {
                    final Iterable<Relationship> targetClassifications = interaction.getEndNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
                    for (Relationship targetClassification : targetClassifications) {
                        final Node targetTaxon = targetClassification.getEndNode();
                        if (!targetTaxon.hasLabel(NodeLabel.Taxon_HasInteractionShortCut)) {
                            final InteractType relType = InteractType.valueOf(interaction.getType().name());
                            createInteraction(sourceTaxon, targetTaxon, relType, false, interaction.getType());
                            createInteraction(targetTaxon, sourceTaxon, InteractType.inverseOf(relType), true, NodeUtil.asNeo4j(InteractType.inverseOf(relType)));
                        }
                    }
                }
            }
        }
        return handled;
    }


    private void createInteraction(Node sourceTaxon, Node targetTaxon, InteractType relType, boolean inverted, RelationshipType relationshipType) {
        final Relationship interactRel = sourceTaxon.createRelationshipTo(targetTaxon, relationshipType);
        NodeUtil.enrichWithInteractProps(relType, interactRel, inverted);
        sourceTaxon.addLabel(NodeLabel.Taxon_HasInteractionShortCut);
    }


}
