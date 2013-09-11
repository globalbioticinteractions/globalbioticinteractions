package org.eol.globi.data;

import junit.framework.Assert;
import org.eol.globi.domain.Specimen;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.Before;
import org.junit.Test;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Taxon;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class NodeFactoryTest extends GraphDBTestCase {

    public static final String EXPECTED_PATH = "kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR;
    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;
    NodeFactory nodeFactory;

    @Before
    public void createFactory() {
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) throws IOException {
            }
        });
    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) throws IOException {
                taxon.setPath(EXPECTED_PATH);
                taxon.setExternalId("anExternalId");
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
            }
        });

        assertThat(getGraphDb().index().existsForNodes("taxonCommonNames"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxonpaths"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxonNameSuggestions"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));

        assertEnrichedPropertiesSet(nodeFactory.getOrCreateTaxon("some name"));
        assertEnrichedPropertiesSet(nodeFactory.findTaxon("some name"));
        IndexHits<Node> hits = nodeFactory.findTaxaByPath("etc");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));
        hits = nodeFactory.findTaxaByCommonName("some german name");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("kingdom");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("phylum");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("some");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("german");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));

        hits = nodeFactory.suggestTaxaByName("@de");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new Taxon(hits.getSingle()));
    }

    private void assertEnrichedPropertiesSet(Taxon aTaxon) {
        assertThat(aTaxon.getPath(), is(EXPECTED_PATH));
        assertThat(aTaxon.getCommonNames(), is(EXPECTED_COMMON_NAMES));
        assertThat(aTaxon.getName(), is("some name"));
        assertThat(aTaxon.getExternalId(), is("anExternalId"));
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
        Taxon taxon = nodeFactory.getOrCreateTaxon("bla bla");
        assertEquals("bla bla", taxon.getName());
    }

    @Test
    public void createSpeciesTwice() throws NodeFactoryException {
        assertFamilyCorrectness("Alpheidae", "Alpheidae");
    }


    @Test
    public void createSpeciesParenthesis() throws NodeFactoryException {
        assertFamilyCorrectness("Alpheidae", "Alphaeidae (lar)");
    }

    @Test
    public void createSpeciesCrypticDescription() throws NodeFactoryException {
        assertFamilyCorrectness("Corophiidae", "Corophiidae Genus A");
    }

    @Test
    public void findCloseMatchForTaxonPath() throws NodeFactoryException {
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", null, "Animalia Mammalia");
        Transaction transaction = homoSapiens.getUnderlyingNode().getGraphDatabase().beginTx();
        transaction.success();
        transaction.finish();
        nodeFactory.getOrCreateTaxon("Homo erectus");
        assertMatch("Mammalia");
        assertMatch("Mammali");
        assertMatch("mammali");
        assertMatch("inmalia");
        IndexHits<Node> hits = nodeFactory.findCloseMatchesForTaxonPath("il");
        assertThat(hits.hasNext(), is(false));
    }

    private void assertMatch(String taxonRankOfClassName) {
        IndexHits<Node> hits = nodeFactory.findCloseMatchesForTaxonPath(taxonRankOfClassName);
        assertThat(hits.hasNext(), is(true));
        Node firstHit = hits.next();
        assertThat((String) firstHit.getProperty(Taxon.NAME), is("Homo sapiens"));
        assertThat((String) firstHit.getProperty(Taxon.PATH), is("Animalia Mammalia"));
        assertThat(hits.hasNext(), is(false));
    }

    @Test
    public void findCloseMatch() throws NodeFactoryException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        IndexHits<Node> hits = nodeFactory.findCloseMatchesForTaxonName("Homo sapiens");
        assertThat(hits.hasNext(), is(true));
        hits.close();
        hits = nodeFactory.findCloseMatchesForTaxonName("Homo saliens");
        assertThat(hits.hasNext(), is(true));
        hits = nodeFactory.findCloseMatchesForTaxonName("Homo");
        assertThat(hits.hasNext(), is(true));
        hits = nodeFactory.findCloseMatchesForTaxonName("homo sa");
        assertThat(hits.hasNext(), is(true));
    }

    private void assertFamilyCorrectness(String expectedOutputName, String inputName) throws NodeFactoryException {
        nodeFactory.getOrCreateTaxon(inputName);
        Taxon taxon = nodeFactory.getOrCreateTaxon(inputName);
        assertEquals(expectedOutputName, taxon.getName());
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


    @Test
    public void describeAndClassifySpecimenImplicit() throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen("some taxon (bla)");
        assertThat(specimen.getOriginalTaxonDescription(), is("some taxon (bla)"));
        assertThat("original taxon descriptions are not indexed", nodeFactory.findTaxon("some taxon (bla)").getName(), is(not("some taxon (bla)")));
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
        Taxon taxon = nodeFactory.getOrCreateTaxon(speciesName);
        Taxon genus = taxon;
        assertEquals("bla", genus.getName());
        assertNull(genus.isA());
    }

    private void assertFamily(String speciesName) throws NodeFactoryException {
        Taxon family = nodeFactory.getOrCreateTaxon(speciesName);
        assertEquals("Blabae", family.getName());
    }
}
