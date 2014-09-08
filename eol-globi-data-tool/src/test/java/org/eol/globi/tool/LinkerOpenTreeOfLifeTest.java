package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class LinkerOpenTreeOfLifeTest extends GraphDBTestCase {

    @Test
    public void frogs() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Anura");
        new LinkerGlobalNames().link(getGraphDb());
        LinkTestUtil.assertHasOther("Anura", 4, nodeFactory);
    }

    @Test
    public void homoSapiens() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink("Homo sapiens", 5, "770315");
    }

    @Test
    public void ariopsisFelis() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink("Ariopsis felis", 6, "139650");
    }

    protected void assertOTTLink(String name, int expectedCount, String ottId) throws NodeFactoryException, PropertyEnricherException {
        OpenTreeTaxonIndex index = null;
        try {
            index = new OpenTreeTaxonIndex(getClass().getResource("taxonomy-small.tsv"));
            nodeFactory.getOrCreateTaxon(name);
            LinkerGlobalNames linkerGlobalNames = new LinkerGlobalNames();
            linkerGlobalNames.link(getGraphDb());
            new LinkerOpenTreeOfLife().linkToOpenTreeOfLife(getGraphDb(), index);
            List<String> externalIds = LinkTestUtil.assertHasOther(name, expectedCount, nodeFactory);
            assertThat(externalIds, hasItem(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix() + ottId));
        } finally {
            if (index != null) {
                index.destroy();
            }
        }
    }

}
