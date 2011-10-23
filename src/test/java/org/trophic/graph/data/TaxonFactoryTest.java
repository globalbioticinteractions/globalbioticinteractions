package org.trophic.graph.data;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.trophic.graph.domain.Family;
import org.trophic.graph.domain.Genus;
import org.trophic.graph.domain.Species;
import org.trophic.graph.domain.Taxon;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TaxonFactoryTest extends GraphDBTestCase {

    TaxonFactory taxonFactory;

    @Before
    public void createFactory() {
        taxonFactory = new TaxonFactory(getGraphDb());
    }

    @Test
    public void createSpecies() throws TaxonFactoryException {
        Taxon taxon = taxonFactory.create("bla bla", null);
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
        taxonFactory.create(inputName, null);
        Taxon taxon = taxonFactory.create(inputName, null);
        Assert.assertEquals(Family.class.getSimpleName(), taxon.getType());
        assertEquals(expectedOutputName, taxon.getName());
    }

    @Test
    public void createSpeciesWithFamily() throws TaxonFactoryException {
        Taxon family = taxonFactory.getOrCreateFamily("theFam");
        Taxon taxon = taxonFactory.create("bla bla", family);
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
        Taxon taxon = taxonFactory.create(speciesName, null);
        Taxon genus = taxon;
        assertEquals("Genus", genus.getType());
        assertEquals("bla", genus.getName());
        assertNull(genus.isPartOf());
    }

    private void assertFamily(String speciesName) throws TaxonFactoryException {
        Taxon family = taxonFactory.create(speciesName, null);
        assertEquals("Family", family.getType());
        assertEquals("Blabae", family.getName());
    }
}
