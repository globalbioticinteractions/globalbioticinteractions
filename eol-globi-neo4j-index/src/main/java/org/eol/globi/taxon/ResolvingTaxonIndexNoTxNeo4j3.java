package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

import java.util.Map;

public class ResolvingTaxonIndexNoTxNeo4j3 extends NonResolvingTaxonIndexNoTxNeo4j3 implements ResolvingTaxonIndex {

    private final PropertyEnricher enricher;

    public ResolvingTaxonIndexNoTxNeo4j3(PropertyEnricher enricher, GraphDatabaseService graphDbService) {
        super(graphDbService);
        this.enricher = enricher;
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

    @Override
    public Taxon getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        Taxon taxonFound = findTaxonById(taxon.getExternalId());

        if (taxonFound == null) {
            taxonFound = findTaxonByName(taxon.getName());
        }

        if (taxonFound == null) {
            try {
                Map<String, String> taxonResolved = enricher.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
                Taxon resolvedOrNoMatch = TaxonUtil.isResolved(taxonResolved)
                        ? TaxonUtil.mapToTaxon(taxonResolved)
                        : TaxonUtil.copyNoMatchTaxon(taxon);

                taxonFound = new TaxonNode(getGraphDbService().createNode());
                TaxonUtil.copy(resolvedOrNoMatch, taxonFound);
            } catch (PropertyEnricherException e) {
                // ignore
            }

        }
        return taxonFound;
    }


    @Override
    public void setIndexResolvedTaxaOnly(boolean indexResolvedOnly) {

    }

    @Override
    public boolean isIndexResolvedOnly() {
        return true;
    }
}
