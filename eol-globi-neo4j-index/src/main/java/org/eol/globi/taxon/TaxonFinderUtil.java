package org.eol.globi.taxon;

import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.util.Optional;

public class TaxonFinderUtil {
    public static TaxonNode findTaxonOrRelated(String key, String value, Taxon taxonContext, Transaction tx) {
        Node foundNode = findNode(key, value, tx, NodeLabel.Taxon, taxonContext);

        if (foundNode == null) {
            foundNode = findNode(key, value, tx, NodeLabel.Taxon_Verbatim, taxonContext);
            if (foundNode != null) {
                try (ResourceIterable<Relationship> relationships = foundNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.ALIGNED_TO))) {
                    Optional<Relationship> first = relationships.stream().findFirst();
                    foundNode = first.map(Relationship::getStartNode).orElse(null);
                }
            }

        }

        return foundNode == null
                ? null
                : new TaxonNode(foundNode);
    }

    private static Node findNode(String key, String value, Transaction transaction, NodeLabel nodeLabel, Taxon taxonContext) {
        Node foundNode = null;
        ResourceIterator<Node> foundNames = transaction
                .findNodes(
                        nodeLabel,
                        key,
                        value
                );
        while (foundNames.hasNext()) {
            Node candidate = foundNames.next();
            TaxonNode taxonA = new TaxonNode(candidate);
            if (!TaxonUtil.likelyHomonym(taxonA, taxonContext)) {
                foundNode = candidate;
                break;
            }
        }
        return foundNode;
    }
}
