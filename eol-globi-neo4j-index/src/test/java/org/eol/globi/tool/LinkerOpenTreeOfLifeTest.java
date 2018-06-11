package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

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
            taxonIndex.getOrCreateTaxon(taxon);
            new LinkerTermMatcher(getGraphDb(), new TermMatcher() {
                @Override
                public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws PropertyEnricherException {
                    for (String name : names) {
                        termMatchListener.foundTaxonForName(1L, name, taxon, NameType.SAME_AS);
                    }
                }

                @Override
                public void findTerms(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
                    for (Term term : terms) {
                        termMatchListener.foundTaxonForName(1L, term.getName(), taxon, NameType.SAME_AS);
                    }
                }
            }).link();
            new LinkerOpenTreeOfLife(getGraphDb(), index).link();
            Collection<String> externalIds = LinkerTestUtil.assertHasOther(taxon.getName(), expectedCount, taxonIndex, RelTypes.SAME_AS);
            assertThat(externalIds, hasItem(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix() + ottId));
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
