package org.eol.globi.data;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.eol.globi.data.taxon.TaxonLookupService;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Taxon;

import java.io.IOException;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class NodeFactoryTest extends GraphDBTestCase {

    NodeFactory nodeFactory;

    @Before
    public void createFactory() {
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonLookupService() {
            @Override
            public long[] lookupTerms(String taxonName) throws IOException {
                return new long[0];
            }

            @Override
            public void destroy() {

            }
        });
    }

    @Test
    public void createFindLocation() {
        Location location = nodeFactory.getOrCreateLocation(1.2d, 1.4d, -1.0d);
        nodeFactory.getOrCreateLocation(2.2d, 1.4d, -1.0d);
        nodeFactory.getOrCreateLocation(1.2d, 2.4d, -1.0d);
        Location locationNoDepth = nodeFactory.getOrCreateLocation(1.5d, 2.8d, null);
        Assert.assertNotNull(location);
        Location location1 = nodeFactory.findLocation(location.getLatitude(), location.getLongitude(), location.getAltitude());
        Assert.assertNotNull(location1);
        Location foundLocationNoDepth = nodeFactory.findLocation(locationNoDepth.getLatitude(), locationNoDepth.getLongitude(), null);
        Assert.assertNotNull(foundLocationNoDepth);
    }

    @Test
    public void createSpecies() throws NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxon("bla bla", null);
        assertEquals("bla bla", taxon.getName());
        assertEquals("bla", taxon.isA().getProperty("name"));
    }

    @Test
    public void createSpeciesTwice() throws NodeFactoryException {
        String alphaeidae = "Alpheidae";
        assertFamilyCorrectness(alphaeidae, alphaeidae);
    }


    @Test
    public void createSpeciesParenthesis() throws NodeFactoryException {
        assertFamilyCorrectness("Alpheidae", "Alphaeidae (lar)");
    }

    @Test
    public void createSpeciesCrypticDescription() throws NodeFactoryException {
        assertFamilyCorrectness("Corophiidae", "Corophiidae Genus A");
    }

    private void assertFamilyCorrectness(String expectedOutputName, String inputName) throws NodeFactoryException {
        nodeFactory.createTaxon(inputName, null);
        Taxon taxon = nodeFactory.createTaxon(inputName, null);
        assertEquals(expectedOutputName, taxon.getName());
    }

    @Test
    public void createSpeciesWithFamily() throws NodeFactoryException {
        Taxon family = nodeFactory.getOrCreateFamily("theFam");
        Taxon taxon = nodeFactory.createTaxon("bla bla", family);
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

    @Test
    public void indexCleanTaxonNamesOnly() throws NodeFactoryException {
        assertNotDirtyName("trailing spaces  ", "trailing spaces");
        assertNotDirtyName("paren(thesis)", "paren");
        assertNotDirtyName("stars--*", "stars--");
    }

    private void assertNotDirtyName(String dirtyName, String cleanName) throws NodeFactoryException {
        Taxon taxonOfType = nodeFactory.getOrCreateTaxon(dirtyName);
        String actualName = taxonOfType.getName();
        assertThat(actualName, is(not(dirtyName)));
        Taxon taxonOfType1 = nodeFactory.findTaxonOfType(cleanName);
        assertNotNull("should be able to lookup clean versions in index, " +
                "expected to find [" + cleanName + "] for \"dirty nane\" [" + dirtyName + "]", taxonOfType1);
    }


    private void assertGenus(String speciesName) throws NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxon(speciesName, null);
        Taxon genus = taxon;
        assertEquals("bla", genus.getName());
        assertNull(genus.isA());
    }

    private void assertFamily(String speciesName) throws NodeFactoryException {
        Taxon family = nodeFactory.createTaxon(speciesName, null);
        assertEquals("Blabae", family.getName());
    }
}
