package org.eol.globi.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.NodeFactoryException;
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

import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ResolvingTaxonIndexTest extends NonResolvingTaxonIndexTest {
    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

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
    public void createSpeciesMatchHigherOrder() throws NodeFactoryException {
        PropertyEnricher enricher = new PropertyEnricher() {

            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                if ("bla bla".equals(taxon.getName())) {
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
        ResolvingTaxonIndex taxonService = createTaxonService(getGraphDb());
        taxonService.setEnricher(enricher);
        this.taxonService = taxonService;
        TaxonNode taxon = this.taxonService.getOrCreateTaxon(new TaxonImpl("bla bla bla"));
        assertEquals("bla bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());
        assertEquals("someInfoUrl", taxon.getExternalUrl());
        assertEquals("someThumbnailUrl", taxon.getThumbnailUrl());

        taxon = this.taxonService.getOrCreateTaxon(new TaxonImpl("bla bla boo"));
        assertEquals("bla bla", taxon.getName());
        assertEquals("a path", taxon.getPath());
        assertEquals("anExternalId", taxon.getExternalId());

        taxon = this.taxonService.getOrCreateTaxon(new TaxonImpl("boo bla"));
        assertEquals("boo bla", taxon.getName());
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertNull(taxon.getPath());
    }

    @Test
    public void indexResolvedOnly() throws NodeFactoryException {
        TaxonNode unresolvedTaxon = getIndex().getOrCreateTaxon(new TaxonImpl("not resolved"));
        assertNotNull(unresolvedTaxon);
        assertFalse(TaxonUtil.isResolved(unresolvedTaxon));

        final ResolvingTaxonIndex indexResolvedOnly = getIndex();
        indexResolvedOnly.setIndexResolvedTaxaOnly(true);
        assertNull(indexResolvedOnly.getOrCreateTaxon(new TaxonImpl("no resolving either", null)));
    }

    public ResolvingTaxonIndex getIndex() {
        return new ResolvingTaxonIndex(new PropertyEnricher() {
                    @Override
                    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                        return new TreeMap<String, String>(properties);
                    }

                    @Override
                    public void shutdown() {

                    }
                }, getGraphDb());
    }

    public static ResolvingTaxonIndex createTaxonService(GraphDatabaseService graphDb) {
        return new ResolvingTaxonIndex(new PropertyEnricher() {
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
        }, graphDb
        );
    }

    @Test
    public final void synonymsAddedToIndexOnce() throws NodeFactoryException {
        ResolvingTaxonIndex taxonService = createTaxonService(getGraphDb());
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
        this.taxonService = taxonService;

        Taxon taxon2 = new TaxonImpl("not pref", null);
        taxon2.setPath(null);
        TaxonNode first = this.taxonService.getOrCreateTaxon(taxon2);
        assertThat(first.getName(), is("preferred"));
        assertThat(first.getPath(), is("one | two | three"));
        assertThat(first.getPathIds(), is("1 | 2 | 3"));
        Taxon taxon1 = new TaxonImpl("not pref", null);
        taxon1.setPath(null);
        TaxonNode second = this.taxonService.getOrCreateTaxon(taxon1);
        assertThat(second.getNodeID(), is(first.getNodeID()));

        TaxonNode third = this.taxonService.getOrCreateTaxon(new TaxonImpl("not pref"));
        assertThat(third.getNodeID(), is(first.getNodeID()));

        TaxonNode foundTaxon = this.taxonService.findTaxonByName("not pref");
        assertThat(foundTaxon.getNodeID(), is(first.getNodeID()));
        foundTaxon = this.taxonService.findTaxonByName("preferred");
        assertThat(foundTaxon.getNodeID(), is(first.getNodeID()));
    }
}
