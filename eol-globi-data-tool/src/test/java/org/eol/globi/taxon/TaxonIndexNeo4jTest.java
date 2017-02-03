package org.eol.globi.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.PassThroughEnricher;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TaxonIndexNeo4jTest extends GraphDBTestCase {
    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

    private TaxonIndexNeo4j taxonService;

    @Before
    public void init() {
        this.taxonService = createTaxonService(getGraphDb());
    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));

        assertEnrichedPropertiesSet(taxonService.getOrCreateTaxon(new TaxonImpl("some name")));
        assertEnrichedPropertiesSet(taxonService.findTaxonByName("some name"));
    }


    private void assertEnrichedPropertiesSet(TaxonNode aTaxon) {
        assertThat(aTaxon.getPath(), is("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getCommonNames(), is(EXPECTED_COMMON_NAMES));
        assertThat(aTaxon.getName(), is("some name"));
        assertThat(aTaxon.getExternalId(), is("anExternalId"));
    }

    @Test
    public void createTaxon() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("bla bla", null);
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertEquals("bla bla", taxon.getName());
    }

    @Test
    public void createTaxonExternalIdIndex() throws NodeFactoryException {
        taxonService = new TaxonIndexNeo4j(new PassThroughEnricher(),
                new CorrectionService() {
                    @Override
                    public String correct(String taxonName) {
                        return taxonName;
                    }
                }, getGraphDb()
        );
        Taxon taxon1 = new TaxonImpl(null, "foo:123");
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonService.findTaxonById("foo:123"), is(notNullValue()));
    }

    @Test
    public void doNotIndexMagicValuesTaxon() throws NodeFactoryException {
        assertNotIndexed(PropertyAndValueDictionary.NO_NAME);
        assertNotIndexed(PropertyAndValueDictionary.NO_MATCH);
    }

    private void assertNotIndexed(String magicValue) throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(magicValue, null);
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonService.findTaxonByName(magicValue), is(nullValue()));
    }

    @Test
    public void createSpeciesMatchHigherOrder() throws NodeFactoryException {
        PropertyEnricher enricher = new PropertyEnricher() {

            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                if ("bla".equals(taxon.getName())) {
                    taxon.setPath("a path");
                    taxon.setExternalId("anExternalId");
                    taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                    taxon.setExternalUrl("someInfoUrl");
                    taxon.setThumbnailUrl("someThumbnailUrl");
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        };
        taxonService.setEnricher(enricher);
        TaxonNode taxon = taxonService.getOrCreateTaxon(new TaxonImpl("bla bla"));
        assertEquals("bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());
        assertEquals("someInfoUrl", taxon.getExternalUrl());
        assertEquals("someThumbnailUrl", taxon.getThumbnailUrl());

        taxon = taxonService.getOrCreateTaxon(new TaxonImpl("bla bla boo"));
        assertEquals("bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());

        taxon = taxonService.getOrCreateTaxon(new TaxonImpl("boo bla"));
        assertEquals("boo bla", taxon.getName());
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxon.getPath());
    }

    @Test
    public void findCloseMatch() throws NodeFactoryException {
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
    public void findNoMatchNoName() throws NodeFactoryException {
        taxonService.getOrCreateTaxon(new TaxonImpl("some name", PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxonService.findTaxonById(PropertyAndValueDictionary.NO_MATCH));

        taxonService.getOrCreateTaxon(new TaxonImpl(PropertyAndValueDictionary.NO_NAME));
        assertNull(taxonService.findTaxonByName(PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxonService.findTaxonByName(PropertyAndValueDictionary.NO_NAME));
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
        TaxonNode taxon = taxonService.getOrCreateTaxon(new TaxonImpl("bla"));
        assertEquals("bla corrected", taxon.getName());

        TaxonNode bla = taxonService.findTaxonByName("bla");
        assertThat(bla.getName(), is("bla corrected"));

        TaxonNode taxonMatch = taxonService.findTaxonByName("bla corrected");
        assertThat(taxonMatch.getName(), is("bla corrected"));
    }

    @Test
    public void indexResolvedOnly() throws NodeFactoryException {
        TaxonNode unresolvedTaxon = getIndex().getOrCreateTaxon(new TaxonImpl("not resolved"));
        assertNotNull(unresolvedTaxon);
        assertFalse(TaxonUtil.isResolved(unresolvedTaxon));

        final TaxonIndexNeo4j indexResolvedOnly = getIndex();
        indexResolvedOnly.setIndexResolvedTaxaOnly(true);
        assertNull(indexResolvedOnly.getOrCreateTaxon(new TaxonImpl("no resolving either", null)));
    }

    public TaxonIndexNeo4j getIndex() {
        return new TaxonIndexNeo4j(new PropertyEnricher() {
                    @Override
                    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                        return new TreeMap<String, String>(properties);
                    }

                    @Override
                    public void shutdown() {

                    }
                }, new CorrectionService() {
                    @Override
                    public String correct(String taxonName) {
                        return taxonName;
                    }
                }, getGraphDb());
    }

    @Test
    public void synonymsAddedToIndexOnce() throws NodeFactoryException {
        taxonService.setEnricher(new PropertyEnricher() {
            private boolean firstTime = true;

            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                if ("not pref".equals(taxon.getName())) {
                    if (!firstTime) {
                        fail("should already have indexed [" + taxon.getName() + "]...");
                    }
                    taxon.setName("preferred");
                    taxon.setExternalId("bla:123");
                    taxon.setPath("one | two | three");
                    taxon.setPathIds("1 | 2 | 3");
                    firstTime = false;
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        });
        Taxon taxon2 = new TaxonImpl("not pref", null);
        taxon2.setPath(null);
        TaxonNode first = taxonService.getOrCreateTaxon(taxon2);
        assertThat(first.getName(), is("preferred"));
        assertThat(first.getPath(), is("one | two | three"));
        assertThat(first.getPathIds(), is("1 | 2 | 3"));
        Taxon taxon1 = new TaxonImpl("not pref", null);
        taxon1.setPath(null);
        TaxonNode second = taxonService.getOrCreateTaxon(taxon1);
        assertThat(second.getNodeID(), is(first.getNodeID()));

        TaxonNode third = taxonService.getOrCreateTaxon(new TaxonImpl("not pref"));
        assertThat(third.getNodeID(), is(first.getNodeID()));

        TaxonNode foundTaxon = taxonService.findTaxonByName("not pref");
        assertThat(foundTaxon.getNodeID(), is(first.getNodeID()));
        foundTaxon = taxonService.findTaxonByName("preferred");
        assertThat(foundTaxon.getNodeID(), is(first.getNodeID()));
    }

    public static TaxonIndexNeo4j createTaxonService(GraphDatabaseService graphDb) {
        return new TaxonIndexNeo4j(new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR);
                taxon.setExternalId("anExternalId");
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        }, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, graphDb
        );
    }
}
