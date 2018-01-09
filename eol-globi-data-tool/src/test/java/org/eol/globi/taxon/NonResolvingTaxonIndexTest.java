package org.eol.globi.taxon;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class NonResolvingTaxonIndexTest extends GraphDBTestCase {
    NonResolvingTaxonIndex taxonService;

    @Before
    public void init() {
        this.taxonService = createTaxonService(getGraphDb());
    }

    @Test
    public final void createTaxon() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("bla bla", null);
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertEquals("bla bla", taxon.getName());
    }

    @Test
    public final void createNullTaxon() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(null, "EOL:1234");
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertEquals("no name", taxon.getName());
    }

    @Test
    public final void createTaxonExternalIdIndex() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(null, "foo:123");
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonService.findTaxonById("foo:123"), is(notNullValue()));
    }

    @Test
    public final void doNotIndexMagicValuesTaxon() throws NodeFactoryException {
        assertNotIndexed(PropertyAndValueDictionary.NO_NAME);
        assertNotIndexed(PropertyAndValueDictionary.NO_MATCH);
    }

    private final void assertNotIndexed(String magicValue) throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(magicValue, null);
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonService.findTaxonByName(magicValue), is(nullValue()));
    }

    @Test
    public final void findCloseMatch() throws NodeFactoryException {
        taxonService.getOrCreateTaxon(new TaxonImpl("Homo sapiens"));
        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonName("Homo sapiens");
        assertThat(hits.hasNext(), is(true));
        hits.close();
        hits = taxonService.findCloseMatchesForTaxonName("Homo saliens");
        assertThat(hits.hasNext(), is(true));
        hits = taxonService.findCloseMatchesForTaxonName("Homo");
        assertThat(hits.hasNext(), is(true));
        hits = taxonService.findCloseMatchesForTaxonName("homo sa");
        assertThat(hits.hasNext(), is(true));
    }

    @Test
    public final void findNoMatchNoName() throws NodeFactoryException {
        taxonService.getOrCreateTaxon(new TaxonImpl("some name", PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxonService.findTaxonById(PropertyAndValueDictionary.NO_MATCH));

        taxonService.getOrCreateTaxon(new TaxonImpl(PropertyAndValueDictionary.NO_NAME));
        assertNull(taxonService.findTaxonByName(PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxonService.findTaxonByName(PropertyAndValueDictionary.NO_NAME));
    }

    private static NonResolvingTaxonIndex createTaxonService(GraphDatabaseService graphDb) {
        return new NonResolvingTaxonIndex(graphDb);
    }
}
