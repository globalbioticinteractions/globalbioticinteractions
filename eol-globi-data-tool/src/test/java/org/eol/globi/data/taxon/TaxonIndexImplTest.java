package org.eol.globi.data.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.PassThroughEnricher;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TaxonIndexImplTest extends GraphDBTestCase {
    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

    private TaxonIndexImpl taxonService;

    @Before
    public void init() {
        this.taxonService = createTaxonService(getGraphDb());
    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));

        assertEnrichedPropertiesSet(taxonService.getOrCreateTaxon("some name", null, null));
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
        TaxonNode taxon = taxonService.getOrCreateTaxon("bla bla", null, null);
        assertThat(taxon, is(notNullValue()));
        assertEquals("bla bla", taxon.getName());
    }

    @Test
    public void createTaxonExternalIdIndex() throws NodeFactoryException {
        taxonService = new TaxonIndexImpl(new PassThroughEnricher(),
                new CorrectionService() {
                    @Override
                    public String correct(String taxonName) {
                        return taxonName;
                    }
                }, getGraphDb()
        );
        TaxonNode taxon = taxonService.getOrCreateTaxon(null, "foo:123", null);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonService.findTaxonById("foo:123"), is(notNullValue()));
    }

    @Test
    public void doNotIndexMagicValuesTaxon() throws NodeFactoryException {
        assertNotIndexed(PropertyAndValueDictionary.NO_NAME);
        assertNotIndexed(PropertyAndValueDictionary.NO_MATCH);
    }

    private void assertNotIndexed(String magicValue) throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon(magicValue, null, null);
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
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

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

        TaxonNode bla = taxonService.findTaxonByName("bla");
        assertThat(bla.getName(), is("bla corrected"));

        TaxonNode taxonMatch = taxonService.findTaxonByName("bla corrected");
        assertThat(taxonMatch.getName(), is("bla corrected"));
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
        TaxonNode first = taxonService.getOrCreateTaxon("not pref", null, null);
        assertThat(first.getName(), is("preferred"));
        assertThat(first.getPath(), is("one | two | three"));
        assertThat(first.getPathIds(), is("1 | 2 | 3"));
        TaxonNode second = taxonService.getOrCreateTaxon("not pref", null, null);
        assertThat(second.getNodeID(), is(first.getNodeID()));

        TaxonNode third = taxonService.getOrCreateTaxon("not pref", null, null);
        assertThat(third.getNodeID(), is(first.getNodeID()));

        TaxonNode foundTaxon = taxonService.findTaxonByName("not pref");
        assertThat(foundTaxon.getNodeID(), is(first.getNodeID()));
        foundTaxon = taxonService.findTaxonByName("preferred");
        assertThat(foundTaxon.getNodeID(), is(first.getNodeID()));
    }

    public static TaxonIndexImpl createTaxonService(GraphDatabaseService graphDb) {
        return new TaxonIndexImpl(new PropertyEnricher() {
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
