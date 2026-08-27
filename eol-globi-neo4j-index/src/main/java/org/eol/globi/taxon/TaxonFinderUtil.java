package org.eol.globi.taxon;

import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonUtil;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

public class TaxonFinderUtil {

    public static TaxonNode findTaxonOrRelated(String key,
                                               String value,
                                               Transaction tx) {
        return findTaxonOrRelated(key, value, tx, null);
    }

    public static TaxonNode findTaxonOrRelated(String key,
                                               String value,
                                               Transaction tx,
                                               Taxon taxonContext) {
        Node foundNode = findNode(key, value, tx, taxonContext);

        return foundNode == null
                ? null
                : new TaxonNode(foundNode);
    }

    private static Node findNode(String key,
                                 String value,
                                 Transaction transaction,
                                 Taxon taxonContext) {
        Node foundNode = null;
        ResourceIterator<Node> foundNames = transaction
                .findNodes(
                        NodeLabel.Taxon,
                        key,
                        value
                );
        while (foundNames.hasNext()) {
            Node candidate = foundNames.next();
            TaxonNode taxonA = new TaxonNode(candidate);
            if (taxonContext == null || !TaxonUtil.likelyHomonym(taxonContext, taxonA)) {
                foundNode = candidate;
                break;
            }
        }
        return foundNode;
    }
}
