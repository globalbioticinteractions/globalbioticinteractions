package org.eol.globi.taxon;

import org.apache.jena.ext.com.google.common.collect.Streams;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                List<Map<String, String>> taxonResolved = enricher.enrichAllMatches(TaxonUtil.taxonToMap(taxon));

                List<TaxonNode> matchCandidates = taxonResolved
                        .stream()
                        .filter(TaxonUtil::isResolved)
                        .map(TaxonUtil::mapToTaxon)
                        .filter(t -> !TaxonUtil.hasLiteratureReference(t))
                        .map(this::taxonNodeFor)
                        .collect(Collectors.toList());

                TaxonNode primary = matchCandidates.size() == 0
                        ? createNoMatch(taxon)
                        : matchCandidates.get(0);
                taxonFound = primary;

                Streams.concat(matchCandidates.stream().skip(1), Stream.of(taxonNodeFor(taxon)))
                .forEach(n -> {
                    n.getUnderlyingNode().createRelationshipTo(primary.getUnderlyingNode(), NodeUtil.asNeo4j(RelTypes.SAME_AS));
                });
            } catch (PropertyEnricherException e) {
                // ignore
            }

        }
        return taxonFound;
    }

    private TaxonNode taxonNodeFor(Taxon r) {
        TaxonNode t = new TaxonNode(getGraphDbService().createNode());
        TaxonUtil.copy(r, t);
        return t;
    }

    private TaxonNode createNoMatch(Taxon taxon) {
        return taxonNodeFor(TaxonUtil.copyNoMatchTaxon(taxon));
    }


    @Override
    public void setIndexResolvedTaxaOnly(boolean indexResolvedOnly) {

    }

    @Override
    public boolean isIndexResolvedOnly() {
        return true;
    }
}
