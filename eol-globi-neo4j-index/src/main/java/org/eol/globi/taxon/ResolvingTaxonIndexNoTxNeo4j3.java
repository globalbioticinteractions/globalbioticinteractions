package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

public class ResolvingTaxonIndexNoTxNeo4j3 extends ResolvingTaxonIndexNoTxNeo4j2 {

    public ResolvingTaxonIndexNoTxNeo4j3(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(enricher, graphDbService);
    }

    @Override
    protected void indexTaxon(Taxon provided, TaxonNode resolved) throws NodeFactoryException {
        if (!StringUtils.equals(provided.getName(), resolved.getName())
                && !StringUtils.equals(provided.getExternalId(), resolved.getExternalId())) {
            if (provided instanceof TaxonNode) {
                ((TaxonNode) provided).getUnderlyingNode().createRelationshipTo(resolved.getUnderlyingNode(), NodeUtil.asNeo4j(RelTypes.SAME_AS));
            }
        }
    }

    @Override
    public TaxonNode findTaxonByName(String name) throws NodeFactoryException {
        return findTaxonOrRelated(PropertyAndValueDictionary.NAME, name, getGraphDbService());
    }

    @Override
    public TaxonNode findTaxonById(String externalId) {
        return findTaxonOrRelated(PropertyAndValueDictionary.EXTERNAL_ID, externalId, getGraphDbService());
    }

    public static TaxonNode findTaxonOrRelated(String key, String value, GraphDatabaseService graphDbService) {
        Node foundNode = null;
        try (ResourceIterator<Node> foundNames = graphDbService
                .findNodes(
                        NodeLabel.Taxon,
                        key,
                        value
                )) {
            while (foundNames.hasNext()) {
                Node next = foundNames.next();
                Iterable<Relationship> rels = next
                        .getRelationships(
                                NodeUtil.asNeo4j(RelTypes.SAME_AS),
                                NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS)
                        );
                for (Relationship rel : rels) {
                    foundNode = rel.getEndNode();
                    if (foundNode != null) {
                        break;
                    }
                }

            }
            return foundNode == null
                    ? null
                    : new TaxonNode(foundNode);
        }
    }


}
