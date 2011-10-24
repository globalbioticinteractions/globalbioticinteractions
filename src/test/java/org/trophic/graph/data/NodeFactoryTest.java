package org.trophic.graph.data;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.trophic.graph.domain.Taxon;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeFactoryTest extends GraphDBTestCase {

    NodeFactory nodeFactory;

    @Before
    public void createFactory() {
        nodeFactory = new NodeFactory(getGraphDb());
    }

    @Test
    public void createSpecies() throws TaxonFactoryException {
        Taxon taxon = nodeFactory.createTaxon("bla bla", null);
        assertEquals("Species", taxon.getType());
        assertEquals("bla bla", taxon.getName());
        assertEquals("bla", taxon.isPartOf().getProperty("name"));
    }

    @Test
    public void createSpeciesTwice() throws TaxonFactoryException {
        String alphaeidae = "Alphaeidae";
        assertFamilyCorrectness(alphaeidae, alphaeidae);
    }


    @Test
    public void createSpeciesParenthesis() throws TaxonFactoryException {
        assertFamilyCorrectness("Alphaeidae", "Alphaeidae (lar)");
    }

    @Test
    public void createSpeciesCrypticDescription() throws TaxonFactoryException {
        assertFamilyCorrectness("Corophiidae", "Corophiidae Genus A");
    }

    private void assertFamilyCorrectness(String expectedOutputName, String inputName) throws TaxonFactoryException {
        nodeFactory.createTaxon(inputName, null);
        Taxon taxon = nodeFactory.createTaxon(inputName, null);
        Assert.assertEquals(Taxon.FAMILY, taxon.getType());
        assertEquals(expectedOutputName, taxon.getName());
    }

    @Test
    public void createSpeciesWithFamily() throws TaxonFactoryException {
        Taxon family = nodeFactory.getOrCreateFamily("theFam");
        Taxon taxon = nodeFactory.createTaxon("bla bla", family);
        Assert.assertEquals("Species", taxon.getType());
        assertEquals("bla bla", taxon.getName());
        Taxon genusTaxon = taxon.isPartOfTaxon();
        assertEquals("bla", genusTaxon.getName());
        assertEquals("theFam", genusTaxon.isPartOfTaxon().getName());
    }

    @Test
    public void createGenus() throws TaxonFactoryException {
        assertGenus("bla sp.");
        assertGenus("bla spp.");
        assertGenus("bla spp. (bla bla)");
    }

    @Test
    public void createFamily() throws TaxonFactoryException {
        assertFamily("Blabae sp.");
        assertFamily("Blabae spp.");
        assertFamily("Blabae spp. (bla bla)");
    }

    private void assertGenus(String speciesName) throws TaxonFactoryException {
        Taxon taxon = nodeFactory.createTaxon(speciesName, null);
        Taxon genus = taxon;
        assertEquals("Genus", genus.getType());
        assertEquals("bla", genus.getName());
        assertNull(genus.isPartOf());
    }

    private void assertFamily(String speciesName) throws TaxonFactoryException {
        Taxon family = nodeFactory.createTaxon(speciesName, null);
        assertEquals("Family", family.getType());
        assertEquals("Blabae", family.getName());
    }
}
