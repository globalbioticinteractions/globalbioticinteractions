package org.eol.globi.data.taxon;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TaxonServiceImplTest extends GraphDBTestCase {
    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

    private TaxonServiceImpl taxonService;

    @Before
    public void init() {
        this.taxonService = createTaxonService();
    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        assertThat(getGraphDb().index().existsForNodes("taxonCommonNames"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxonpaths"), is(true));
        assertThat(getGraphDb().index().existsForNodes("taxonNameSuggestions"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));

        assertEnrichedPropertiesSet(taxonService.getOrCreateTaxon("some name", null, null));
        assertEnrichedPropertiesSet(taxonService.findTaxon("some name"));
        IndexHits<Node> hits = taxonService.findTaxaByPath("etc");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));
        hits = taxonService.findTaxaByCommonName("some german name");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = taxonService.suggestTaxaByName("kingdom");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = taxonService.suggestTaxaByName("phylum");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = taxonService.suggestTaxaByName("some");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = taxonService.suggestTaxaByName("german");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));

        hits = taxonService.suggestTaxaByName("@de");
        assertThat(hits.size(), is(1));
        assertEnrichedPropertiesSet(new TaxonNode(hits.getSingle()));
    }


    private void assertEnrichedPropertiesSet(TaxonNode aTaxon) {
        assertThat(aTaxon.getPath(), is("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getCommonNames(), is(EXPECTED_COMMON_NAMES));
        assertThat(aTaxon.getName(), is("some name"));
        assertThat(aTaxon.getExternalId(), is("anExternalId"));
    }

    @Test
    public void findByStringWithWhitespaces() throws NodeFactoryException {
        TaxonPropertyEnricher enricher = new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "Homo sapiens" + CharsetConstant.SEPARATOR);
                taxon.setExternalId("anExternalId");
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                taxon.setName("this is the actual name");
            }
        };
        taxonService.setEnricher(enricher);
        taxonService.getOrCreateTaxon("Homo sapiens", null, null);

        assertThat(getGraphDb().index().existsForNodes("taxonNameSuggestions"), is(true));
        Index<Node> index = getGraphDb().index().forNodes("taxonNameSuggestions");
        Query query = new TermQuery(new Term("name", "name"));
        IndexHits<Node> hits = index.query(query);
        assertThat(hits.size(), is(1));

        hits = index.query("name", "s nme~");
        assertThat(hits.size(), is(1));

        hits = index.query("name", "geRman~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:geRman~ AND name:som~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:hmo~ AND name:SApiens~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:hmo~ AND name:sapiens~");
        assertThat(hits.size(), is(1));

        // queries are case sensitive . . . should all be lower cased.
        hits = index.query("name:HMO~ AND name:saPIENS~");
        assertThat(hits.size(), is(0));
    }

    @Test
    public void createTaxon() throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon("bla bla", null, null);
        assertThat(taxon, is(notNullValue()));
        assertEquals("bla bla", taxon.getName());
    }

    @Test
    public void createSpeciesMatchHigherOrder() throws NodeFactoryException {
        TaxonPropertyEnricher enricher = new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                if ("bla".equals(taxon.getName())) {
                    taxon.setPath("a path");
                    taxon.setExternalId("anExternalId");
                    taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                }
            }
        };
        taxonService.setEnricher(enricher);
        TaxonNode taxon = taxonService.getOrCreateTaxon("bla bla", null, null);
        assertEquals("bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());

        taxon = taxonService.getOrCreateTaxon("bla bla boo", null, null);
        assertEquals("bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());

        taxon = taxonService.getOrCreateTaxon("boo bla", null, null);
        assertEquals("boo bla", taxon.getName());
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxon.getPath());
    }

    @Test
    public void findCloseMatchForTaxonPath() throws NodeFactoryException {
        taxonService.setEnricher(new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {

            }
        });
        taxonService.getOrCreateTaxon("Homo sapiens", "someid", "Animalia Mammalia");
        taxonService.getOrCreateTaxon("Homo erectus", null, null);
        assertMatch("Mammalia");
        assertMatch("Mammali");
        assertMatch("mammali");
        assertMatch("inmalia");
        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonPath("il");
        assertThat(hits.hasNext(), is(false));
    }

    private void assertMatch(String taxonRankOfClassName) {
        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonPath(taxonRankOfClassName);
        assertThat(hits.hasNext(), is(true));
        Node firstHit = hits.next();
        assertThat((String) firstHit.getProperty(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat((String) firstHit.getProperty(PropertyAndValueDictionary.PATH), is("Animalia Mammalia"));
        assertThat(hits.hasNext(), is(false));
    }

    @Test
    public void findCloseMatch() throws NodeFactoryException {
        taxonService.getOrCreateTaxon("Homo sapiens", null, null);
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
    public void ensureCorrectedIndexing() throws NodeFactoryException {
        taxonService.setCorrector(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                String corrected = taxonName;
                if (!taxonName.endsWith("corrected")) {
                    corrected = taxonName + " corrected";
                }
                return corrected;
            }
        });
        TaxonNode taxon = taxonService.getOrCreateTaxon("bla", null, null);
        assertEquals("bla corrected", taxon.getName());

        TaxonNode bla = taxonService.findTaxon("bla");
        assertThat(bla.getName(), is("bla corrected"));

        TaxonNode taxonMatch = taxonService.findTaxon("bla corrected");
        assertThat(taxonMatch.getName(), is("bla corrected"));
    }

    private TaxonServiceImpl createTaxonService() {
        return new TaxonServiceImpl(new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR);
                taxon.setExternalId("anExternalId");
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
            }
        }, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb()
        );
    }
}
