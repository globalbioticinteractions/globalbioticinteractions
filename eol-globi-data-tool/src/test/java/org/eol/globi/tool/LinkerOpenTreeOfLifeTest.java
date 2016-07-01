package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class LinkerOpenTreeOfLifeTest extends GraphDBTestCase {

    @Test
    public void homoSapiens() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink("Homo sapiens", 6, "770315");
    }

    @Test
    public void ariopsisFelis() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink("Ariopsis felis", 6, "139650");
    }

    protected void assertOTTLink(String name, int expectedCount, String ottId) throws NodeFactoryException, PropertyEnricherException {
        OpenTreeTaxonIndex index = null;
        try {
            index = new OpenTreeTaxonIndex(getClass().getResource("taxonomy-small.tsv"));
            taxonIndex.getOrCreateTaxon(name);
            LinkerGlobalNames linkerGlobalNames = new LinkerGlobalNames();
            linkerGlobalNames.link(getGraphDb());
            new LinkerOpenTreeOfLife().link(getGraphDb(), index);
            List<String> externalIds = LinkerTestUtil.assertHasOther(name, expectedCount, taxonIndex, RelTypes.SAME_AS);
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
