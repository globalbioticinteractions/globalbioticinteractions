package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LinkerOpenTreeOfLifeTest extends GraphDBTestCase {

    @Test
    public void homoSapiens() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink(2, "770315", new TaxonImpl("Homo sapiens", "GBIF:2436436"));
    }

    @Test
    public void ariopsisFelis() throws NodeFactoryException, PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl("Ariopsis felis", "GBIF:5202927");
        taxon.setPath("path0 | path1");
        assertOTTLink(2, "139650", taxon);
    }

    protected void assertOTTLink(int expectedCount, String ottId, Taxon taxon) throws NodeFactoryException, PropertyEnricherException {
        OpenTreeTaxonIndex index = null;
        try {
            index = new OpenTreeTaxonIndex(getClass().getResource("taxonomy-small.tsv"));
            Taxon createdTaxon = taxonIndex.getOrCreateTaxon(taxon);
            long nodeID = ((NodeBacked) createdTaxon).getNodeID();

            new LinkerTermMatcher((terms, termMatchListener) -> {
                for (Term term : terms) {
                    termMatchListener.foundTaxonForTerm(nodeID, term, taxon, NameType.SAME_AS);
                }
            }).index(new GraphServiceFactoryProxy(getGraphDb()));
            new LinkerOpenTreeOfLife(index).index(new GraphServiceFactoryProxy(getGraphDb()));

            Transaction transaction = getGraphDb().beginTx();
            Collection<String> externalIds = LinkerTestUtil.assertHasOther(taxon.getName(), expectedCount, taxonIndex, RelTypes.SAME_AS);
            assertThat(externalIds, hasItem(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix() + ottId));
            transaction.success();
            transaction.close();
        } finally {
            if (index != null) {
                index.destroy();
            }
        }
    }

    @Test
    public void copyAndLink() {
        final TaxonImpl taxon = new TaxonImpl();
        taxon.setExternalId("GBIF:123");
        final Taxon linkedTaxon = LinkerOpenTreeOfLife.copyAndLinkToOpenTreeTaxon(taxon, 555L);
        assertThat(linkedTaxon.getExternalId(), is("OTT:555"));
        assertThat(linkedTaxon.getExternalUrl(), is("https://tree.opentreeoflife.org/opentree/ottol@555"));
    }

}
