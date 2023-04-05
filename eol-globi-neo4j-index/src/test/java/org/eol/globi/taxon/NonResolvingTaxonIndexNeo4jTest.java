package org.eol.globi.taxon;

import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NonResolvingTaxonIndexNeo4jTest extends GraphDBNeo4jTestCase {
    private NonResolvingTaxonIndexNeo4j2 taxonService;

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
    public final void getOrCreateNullTaxon() throws NodeFactoryException {
        assertThat(taxonService.getOrCreateTaxon(null), is(nullValue()));
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
    public void createTaxonWithExplicitRanks() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("foo", "foo:123");
        taxon1.setPath("a kingdom name | a phylum name | boo name | a class name | an order name | a family name | a genus name | a subgenus name | a species name");
        taxon1.setPathIds("a kingdom id | a phylum id | boo id | a class id | an order id | a family id | a genus id | a subgenus id | a species id");
        taxon1.setPathNames("kingdom | phylum | boo | class | order | family | genus | subgenus | species");
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);

        assertThat(propertyOf(taxon, "kingdomName"), is("a kingdom name"));
        assertThat(propertyOf(taxon, "kingdomId"), is("a kingdom id"));
        assertThat(propertyOf(taxon, "phylumName"), is("a phylum name"));
        assertThat(propertyOf(taxon, "phylumId"), is("a phylum id"));

        assertThat(propertyOf(taxon, "orderName"), is("an order name"));
        assertThat(propertyOf(taxon, "orderId"), is("an order id"));

        assertThat(propertyOf(taxon, "className"), is("a class name"));
        assertThat(propertyOf(taxon, "classId"), is("a class id"));
        assertThat(propertyOf(taxon, "familyName"), is("a family name"));
        assertThat(propertyOf(taxon, "familyId"), is("a family id"));

        assertThat(propertyOf(taxon, "genusName"), is("a genus name"));
        assertThat(propertyOf(taxon, "genusId"), is("a genus id"));

        assertThat(propertyOf(taxon, "subgenusName"), is("a subgenus name"));
        assertThat(propertyOf(taxon, "subgenusId"), is("a subgenus id"));

        assertThat(propertyOf(taxon, "speciesName"), is("a species name"));
        assertThat(propertyOf(taxon, "speciesId"), is("a species id"));
    }

    static Object propertyOf(TaxonNode taxon, String propertyName) {
        return taxon.getUnderlyingNode().getProperty(propertyName);
    }

    @Test
    public final void doNotIndexMagicValuesTaxon() throws NodeFactoryException {
        assertNotIndexed(PropertyAndValueDictionary.NO_NAME);
        assertNotIndexed(PropertyAndValueDictionary.NO_MATCH);
        assertNotIndexed(PropertyAndValueDictionary.AMBIGUOUS_MATCH);
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

    @Ignore("disable homonym detection methods for now; related to https://github.com/globalbioticinteractions/globalbioticinteractions/issues/871")
    @Test
    public final void indexTwoHomonymsSeparately() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("some name 4567", null);
        taxon1.setPath("one | two | three | some name 4567");
        taxon1.setPathNames("kingdom | family | genus | species");

        assertThat(taxonService.findTaxon(taxon1), is(nullValue()));

        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));

        assertThat(taxonService.findTaxon(taxon1), is(not(nullValue())));
        assertThat(taxonService.findTaxonByName("some name 4567"), is(notNullValue()));

        Taxon taxon2 = new TaxonImpl("some name 4567", null);
        taxon2.setPath("four | five | six | some name 4567");
        taxon2.setPathNames("kingdom | family | genus | species");

        assertThat(taxonService.findTaxon(taxon2), is(nullValue()));
    }

    @Ignore("disable homonym detection methods for now; related to https://github.com/globalbioticinteractions/globalbioticinteractions/issues/871")
    @Test
    public final void indexTwoHomonymsSeparately2() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("some name", "foo:123");
        taxon1.setPath("seven | eight | nine | some name");
        taxon1.setPathNames("kingdom | family | genus | species");
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon.getPath(), is("seven | eight | nine | some name"));
        assertThat(taxonService.findTaxon(taxon1), is(not(nullValue())));

        assertThat(taxonService.findTaxonById("foo:123"), is(notNullValue()));

        Taxon taxon2 = new TaxonImpl("some name", "foo:123");
        taxon2.setPath("ten | eleven | twelve | some name");
        taxon2.setPathNames("kingdom | family | genus | species");
        TaxonNode homonym = taxonService.getOrCreateTaxon(taxon2);
        assertThat(homonym.getNodeID(), is(not(taxon.getNodeID())));

        Taxon taxon3 = new TaxonImpl("some name");

        assertThat(taxonService.findTaxon(taxon3).getPath(), is("seven | eight | nine | some name"));
    }

    @Test
    public final void indexHomonymExplicitly() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("some name", "foo:123");
        taxon1.setPath("one | two | three | some name");
        taxon1.setPathNames("kingdom | family | genus | species");
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonService.findTaxon(taxon1), is(not(nullValue())));

        assertThat(taxonService.findTaxonById("foo:123"), is(notNullValue()));

        Taxon taxon2 = new TaxonImpl("some name", "foo:123");
        taxon2.setPath("some name");
        taxon2.setPathNames("species");
        assertThat(taxonService.findTaxon(taxon2), is(not(nullValue())));
    }


    private static NonResolvingTaxonIndexNeo4j2 createTaxonService(GraphDatabaseService graphDb) {
        return new NonResolvingTaxonIndexNeo4j2(graphDb);
    }
}
