package org.trophic.graph.data;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.trophic.graph.domain.Taxon;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class NodeFactoryTest extends GraphDBTestCase {

    NodeFactory nodeFactory;

    @Before
    public void createFactory() {
        nodeFactory = new NodeFactory(getGraphDb());
    }

    @Test
    public void createSpecies() throws NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxon("bla bla", null);
        assertEquals(Taxon.SPECIES, taxon.getType());
        assertEquals("bla bla", taxon.getName());
        assertEquals("bla", taxon.isA().getProperty("name"));
    }

    @Test
    public void createSpeciesTwice() throws NodeFactoryException {
        String alphaeidae = "Alphaeidae";
        assertFamilyCorrectness(alphaeidae, alphaeidae);
    }


    @Test
    public void createSpeciesParenthesis() throws NodeFactoryException {
        assertFamilyCorrectness("Alphaeidae", "Alphaeidae (lar)");
    }

    @Test
    public void createSpeciesCrypticDescription() throws NodeFactoryException {
        assertFamilyCorrectness("Corophiidae", "Corophiidae Genus A");
    }

    private void assertFamilyCorrectness(String expectedOutputName, String inputName) throws NodeFactoryException {
        nodeFactory.createTaxon(inputName, null);
        Taxon taxon = nodeFactory.createTaxon(inputName, null);
        Assert.assertEquals(Taxon.FAMILY, taxon.getType());
        assertEquals(expectedOutputName, taxon.getName());
    }

    @Test
    public void createSpeciesWithFamily() throws NodeFactoryException {
        Taxon family = nodeFactory.getOrCreateFamily("theFam");
        Taxon taxon = nodeFactory.createTaxon("bla bla", family);
        Assert.assertEquals(Taxon.SPECIES, taxon.getType());
        assertEquals("bla bla", taxon.getName());
        Taxon genusTaxon = taxon.isPartOfTaxon();
        assertEquals("bla", genusTaxon.getName());
        assertEquals("theFam", genusTaxon.isPartOfTaxon().getName());
    }

    @Test
    public void createGenus() throws NodeFactoryException {
        assertGenus("bla sp.");
        assertGenus("bla spp.");
        assertGenus("bla spp. (bla bla)");
    }

    @Test
    public void createFamily() throws NodeFactoryException {
        assertFamily("Blabae sp.");
        assertFamily("Blabae spp.");
        assertFamily("Blabae spp. (bla bla)");
    }

    private void assertGenus(String speciesName) throws NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxon(speciesName, null);
        Taxon genus = taxon;
        assertEquals(Taxon.GENUS, genus.getType());
        assertEquals("bla", genus.getName());
        assertNull(genus.isA());
    }

    private void assertFamily(String speciesName) throws NodeFactoryException {
        Taxon family = nodeFactory.createTaxon(speciesName, null);
        assertEquals(Taxon.FAMILY, family.getType());
        assertEquals("Blabae", family.getName());
    }
}
