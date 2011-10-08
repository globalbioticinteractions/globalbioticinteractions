package org.trophic.graph.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.domain.Family;
import org.trophic.graph.domain.Genus;
import org.trophic.graph.domain.Species;
import org.trophic.graph.domain.Taxon;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/base-test-context.xml"})
@Transactional
public class TaxonFactoryTest {

    @Autowired
    TaxonFactory taxonFactory;

    @Test
    public void createSpecies() throws TaxonFactoryException {
        Taxon taxon = taxonFactory.create("bla bla", null);
        assertTrue(taxon instanceof Species);
        Species species = (Species) taxon;
        assertEquals("bla bla", species.getName());
        assertEquals("bla", species.getGenus().getName());
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
        taxonFactory.create(inputName, null);
        Taxon taxon = taxonFactory.create(inputName, null);
        assertTrue(taxon instanceof Family);
        Family family = (Family) taxon;
        assertEquals(expectedOutputName, family.getName());
    }

    @Test
    public void createSpeciesWithFamily() throws TaxonFactoryException {
        Family family = new Family("theFam").persist();
        Taxon taxon = taxonFactory.create("bla bla", family);
        assertTrue(taxon instanceof Species);
        Species species = (Species) taxon;
        assertEquals("bla bla", species.getName());
        assertEquals("bla", species.getGenus().getName());
        assertEquals("theFam", species.getGenus().getFamily().getName());
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
        Taxon taxon = taxonFactory.create(speciesName, null);
        assertTrue(taxon instanceof Genus);
        Genus genus = (Genus) taxon;
        assertEquals("bla", genus.getName());
        assertNull(genus.getFamily());
    }
    private void assertFamily(String speciesName) throws TaxonFactoryException {
        Taxon taxon = taxonFactory.create(speciesName, null);
        assertTrue(taxon instanceof Family);
        Family family = (Family) taxon;
        assertEquals("Blabae", family.getName());
    }
}
