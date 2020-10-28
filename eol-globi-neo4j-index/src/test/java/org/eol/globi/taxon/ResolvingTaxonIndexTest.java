package org.eol.globi.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherSingle;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.LinkerTaxonIndex;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.tool.LinkerTaxonIndex.INDEX_TAXON_NAMES_AND_IDS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ResolvingTaxonIndexTest extends NonResolvingTaxonIndexTest {
    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

    @Before
    public void init() {
        this.taxonService = createTaxonService(getGraphDb());
    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        Transaction transaction = getGraphDb().beginTx();
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));
        transaction.success();
        transaction.close();

        assertEnrichedPropertiesSet(taxonService.getOrCreateTaxon(new TaxonImpl("some name")), "");
        assertEnrichedPropertiesSet(taxonService.findTaxonByName("some name"), "");
    }

    @Test
    public void ensureThatEnrichedPropertiesAreLinked() throws NodeFactoryException {
        this.taxonService = new ResolvingTaxonIndex(new PropertyEnricher() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return enrichAllMatches(properties).get(0);
            }

            @Override
            public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon1 = enrichTaxonWithSuffix(properties, "1");
                Taxon taxon2 = enrichTaxonWithSuffix(properties, "2");
                return Arrays.asList(
                        TaxonUtil.taxonToMap(taxon1),
                        TaxonUtil.taxonToMap(taxon2)
                );
            }

            Taxon enrichTaxonWithSuffix(Map<String, String> properties, String suffix) {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                taxon.setPathNames("kingdom" + suffix + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR);
                taxon.setPath("a kingdom name" + suffix + CharsetConstant.SEPARATOR + "a phylum name" + CharsetConstant.SEPARATOR + "a etc name" + CharsetConstant.SEPARATOR);
                taxon.setPathIds("a kingdom id" + suffix + CharsetConstant.SEPARATOR + "a phylum id" + CharsetConstant.SEPARATOR + "a etc id" + CharsetConstant.SEPARATOR);
                taxon.setExternalId("anExternalId" + suffix);
                taxon.setCommonNames(EXPECTED_COMMON_NAMES);
                return taxon;
            }

            @Override
            public void shutdown() {

            }
        }, getGraphDb()
        );


        Transaction transaction = getGraphDb().beginTx();
        assertThat(getGraphDb().index().existsForNodes("taxons"), is(true));
        assertThat(getGraphDb().index().existsForNodes("thisDoesnoTExist"), is(false));
        transaction.success();
        transaction.close();

        TaxonNode indexedTaxonNode = taxonService.getOrCreateTaxon(new TaxonImpl("some name1"));
        assertEnrichedPropertiesSet(indexedTaxonNode, "1");
        TaxonNode someFoundTaxonNode = taxonService.findTaxonByName("some name1");
        assertThat(someFoundTaxonNode.getNodeID(), is(indexedTaxonNode.getNodeID()));
        assertEnrichedPropertiesSet(someFoundTaxonNode, "1");


        try (Transaction ignored = getGraphDb().beginTx()) {
            Index<Node> ids = getGraphDb().index().forNodes(INDEX_TAXON_NAMES_AND_IDS,
                    MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));

            assertThat(
                    ids.query("path:\"some name2\"").size(),
                    is(0)
            );
        }

        LinkerTaxonIndex linkerTaxonIndex = new LinkerTaxonIndex();
        linkerTaxonIndex.index(new GraphServiceFactoryProxy(getGraphDb()));

        try (Transaction ignored = getGraphDb().beginTx()) {
            Index<Node> ids = getGraphDb().index().forNodes(INDEX_TAXON_NAMES_AND_IDS,
                    MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
            IndexHits<Node> hits = ids.query("path:\"a kingdom name2\"");
            assertThat(hits.size(), is(1));
            for (Node hit : hits) {
                TaxonNode taxonHit = new TaxonNode(hit);
                assertNotNull(taxonHit);
                assertThat(taxonHit.getNodeID(), is(indexedTaxonNode.getNodeID()));
            }
        }

        TaxonNode someOtherFoundTaxonNodeTake2 = taxonService.findTaxonByName("some name2");
        assertNull(someOtherFoundTaxonNodeTake2);

    }


    @Test
    public void noMatch() throws NodeFactoryException {
        this.taxonService = new ResolvingTaxonIndex(new PropertyEnricher() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return properties;
            }

            @Override
            public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
                return Collections.emptyList();
            }

            @Override
            public void shutdown() {

            }
        }, getGraphDb()
        ) {{
            setIndexResolvedTaxaOnly(true);
        }};

        TaxonNode indexedTaxonNode = taxonService.getOrCreateTaxon(new TaxonImpl("some name1"));
        assertThat(indexedTaxonNode, is(nullValue()));
    }


    private void assertEnrichedPropertiesSet(TaxonNode aTaxon, String suffix) {
        assertNotNull(aTaxon);
        assertThat(aTaxon.getPathNames(), is("kingdom" + suffix + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getPath(), is("a kingdom name" + suffix + CharsetConstant.SEPARATOR + "a phylum name" + CharsetConstant.SEPARATOR + "a etc name" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getPathIds(), is("a kingdom id" + suffix + CharsetConstant.SEPARATOR + "a phylum id" + CharsetConstant.SEPARATOR + "a etc id" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getCommonNames(), is(EXPECTED_COMMON_NAMES));
        assertThat(aTaxon.getName(), is("some name" + suffix));
        assertThat(aTaxon.getExternalId(), is("anExternalId" + suffix));
    }


    @Test
    public void createSpeciesMatchHigherOrder() throws NodeFactoryException {
        PropertyEnricher enricher = new PropertyEnricherSingle() {

            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
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

    @Test
    public void createTaxonWithExplicitRanks() throws NodeFactoryException {
        ((ResolvingTaxonIndex) this.taxonService).setEnricher(new PropertyEnricherSingle() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return properties;
            }

            @Override
            public void shutdown() {

            }
        });
        super.createTaxonWithExplicitRanks();
    }

    public ResolvingTaxonIndex getIndex() {
        return new ResolvingTaxonIndex(new PropertyEnricherSingle() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return new TreeMap<>(properties);
            }

            @Override
            public void shutdown() {

            }
        }, getGraphDb());
    }

    private static ResolvingTaxonIndex createTaxonService(GraphDatabaseService graphDb) {
        return new ResolvingTaxonIndex(new PropertyEnricherSingle() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon = TaxonUtil.mapToTaxon(properties);
                taxon.setPathNames("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR);
                taxon.setPath("a kingdom name" + CharsetConstant.SEPARATOR + "a phylum name" + CharsetConstant.SEPARATOR + "a etc name" + CharsetConstant.SEPARATOR);
                taxon.setPathIds("a kingdom id" + CharsetConstant.SEPARATOR + "a phylum id" + CharsetConstant.SEPARATOR + "a etc id" + CharsetConstant.SEPARATOR);
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
        taxonService.setEnricher(new PropertyEnricherSingle() {
            private boolean firstTime = true;

            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
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
