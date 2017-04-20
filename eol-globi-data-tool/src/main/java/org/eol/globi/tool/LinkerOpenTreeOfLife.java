package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LinkerOpenTreeOfLife implements Linker {

    private static final Log LOG = LogFactory.getLog(LinkerOpenTreeOfLife.class);
    private final GraphDatabaseService graphDb;
    private final OpenTreeTaxonIndex index;

    public LinkerOpenTreeOfLife(GraphDatabaseService graphDb, OpenTreeTaxonIndex index){
        this.graphDb = graphDb;
        this.index = index;
    }

    public void link() {
            Index<Node> taxons = graphDb.index().forNodes("taxons");
            IndexHits<Node> hits = taxons.query("*:*");
            for (Node hit : hits) {
                Iterable<Relationship> rels = hit.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.SAME_AS));
                Map<String, Long> ottIds = new HashMap<String, Long>();
                for (Relationship rel : rels) {
                    if (link(graphDb, index, ottIds, rel)) {
                        break;
                    }

                }
                validate(ottIds);
            }


        }

        protected void validate(Map<String, Long> ottIds) {
            if (ottIds.size() > 1) {
                Set<Long> uniqueIds = new HashSet<Long>(ottIds.values());
                if (uniqueIds.size() > 1) {
                    LOG.error("found mismatching ottIds for sameAs taxa with ids: [" + ottIds + "]");
                }

            }
        }

        protected boolean link(GraphDatabaseService graphDb, OpenTreeTaxonIndex index, Map<String, Long> ottIds, Relationship rel) {
            boolean hasPrexistingLink = false;
            TaxonNode taxonNode = new TaxonNode(rel.getEndNode());
            String externalId = taxonNode.getExternalId();
            if (StringUtils.contains(externalId, TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix())) {
                ottIds.clear();
                hasPrexistingLink = true;
            } else {
                externalId = StringUtils.replace(externalId, TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), "irmng:");
                externalId = StringUtils.replace(externalId, TaxonomyProvider.INDEX_FUNGORUM.getIdPrefix(), "if:");
                externalId = StringUtils.replace(externalId, TaxonomyProvider.GBIF.getIdPrefix(), "gbif:");
                externalId = StringUtils.replace(externalId, TaxonomyProvider.NCBI.getIdPrefix(), "ncbi:");
                Long ottId = index.findOpenTreeTaxonIdFor(externalId);
                if (ottId != null) {
                    if (ottIds.size() == 0) {
                        Taxon taxonCopy = copyAndLinkToOpenTreeTaxon(taxonNode, ottId);
                        NodeUtil.connectTaxa(taxonCopy, new TaxonNode(rel.getStartNode()), graphDb, RelTypes.SAME_AS);
                    }
                    ottIds.put(externalId, ottId);
                }
            }
            return hasPrexistingLink;
        }

    protected static Taxon copyAndLinkToOpenTreeTaxon(Taxon taxon, Long ottId) {
        Taxon taxonCopy = TaxonUtil.copy(taxon);
        final String externalId = TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix() + ottId;
        taxonCopy.setExternalId(externalId);
        taxonCopy.setExternalUrl(ExternalIdUtil.urlForExternalId(externalId));
        return taxonCopy;
    }
}
