package org.eol.globi.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.PropertyEnricherNoop;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherSingle;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.TaxonIndexFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ResolvingTaxonIndexImplTest extends GraphDBTestCase {

    private TaxonIndexFactory factory;

    public static final String EXPECTED_COMMON_NAMES = "some german name @de" + CharsetConstant.SEPARATOR + "some english name @en" + CharsetConstant.SEPARATOR;

    @Before
    public void init() {
        this.factory = createTaxonService();
    }

    @Test
    public void ensureThatEnrichedPropertiesAreIndexed() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex resolvingTaxonIndex = factory.create(tx);
            assertEnrichedPropertiesSet(resolvingTaxonIndex.getOrCreateTaxon(new TaxonImpl("some name")), "");
            assertEnrichedPropertiesSet(resolvingTaxonIndex.findTaxonByName("some name"), "");
        }
    }

    @Ignore
    @Test
    public void ensureThatEnrichedPropertiesAreLinked() throws StudyImporterException {
        TaxonIndexFactory factory = new TaxonIndexFactory() {
            @Override
            public ResolvingTaxonIndex create(Transaction tx) {
                return new ResolvingTaxonIndexImpl(new PropertyEnricher() {
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
                }, tx);
            }
        };


        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonService = taxonIndexFactory.create(tx);
            Taxon indexedTaxonNode = taxonService.getOrCreateTaxon(new TaxonImpl("some name1"));

            assertEnrichedPropertiesSet(indexedTaxonNode, "1");
            Taxon someFoundTaxonNode = taxonService.findTaxonByName("some name1");
            assertEnrichedPropertiesSet(someFoundTaxonNode, "1");

            Taxon someOtherFoundTaxonNodeTake2 = taxonService.findTaxonByName("some name2");
            assertNull(someOtherFoundTaxonNodeTake2);
        }
    }


    @Test
    public void noMatch() throws NodeFactoryException {
        TaxonIndexFactory factory = new TaxonIndexFactory() {

            @Override
            public ResolvingTaxonIndex create(Transaction tx) {
                ResolvingTaxonIndexImpl resolvingTaxonIndex = new ResolvingTaxonIndexImpl(new PropertyEnricher() {
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
                }, tx);
                resolvingTaxonIndex.setIndexResolvedTaxaOnly(true);
                return resolvingTaxonIndex;
            }
        };

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon indexedTaxonNode = factory.create(tx).getOrCreateTaxon(new TaxonImpl("some name1"));
            assertThat(indexedTaxonNode, is(nullValue()));
        }
    }


    private void assertEnrichedPropertiesSet(Taxon aTaxon, String suffix) {
        assertNotNull(aTaxon);
        assertThat(aTaxon.getPathNames(), is("kingdom" + suffix + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "etc" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getPath(), is("a kingdom name" + suffix + CharsetConstant.SEPARATOR + "a phylum name" + CharsetConstant.SEPARATOR + "a etc name" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getPathIds(), is("a kingdom id" + suffix + CharsetConstant.SEPARATOR + "a phylum id" + CharsetConstant.SEPARATOR + "a etc id" + CharsetConstant.SEPARATOR));
        assertThat(aTaxon.getCommonNames(), is(EXPECTED_COMMON_NAMES));
        assertThat(aTaxon.getName(), is("some name" + suffix));
        assertThat(aTaxon.getExternalId(), is("anExternalId" + suffix));
        assertThat(aTaxon, is(instanceOf(TaxonNode.class)));
        assertThat(((TaxonNode)aTaxon).getUnderlyingNode().getProperty(PropertyAndValueDictionary.EXTERNAL_IDS).toString(), is("|  | a etc id | a etc name | a kingdom id | a kingdom name | a phylum id | a phylum name | anExternalId | some name |"));
        assertThat(((TaxonNode)aTaxon).getUnderlyingNode().getProperty(PropertyAndValueDictionary.NAME_IDS).toString(), is("| anExternalId |"));
        assertThat(((TaxonNode)aTaxon).getUnderlyingNode().getProperty("kingdomId").toString(), is("a kingdom id"));
        assertThat(((TaxonNode)aTaxon).getUnderlyingNode().getProperty("kingdomName").toString(), is("a kingdom name"));
    }


    @Ignore
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
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonService = createTaxonService(tx, enricher);
            Taxon taxon = taxonService.getOrCreateTaxon(new TaxonImpl("bla bla bla"));
            assertEquals("bla bla", taxon.getName());
            assertEquals("a path", taxon.getPath());
            assertEquals("anExternalId", taxon.getExternalId());
            assertEquals("someInfoUrl", taxon.getExternalUrl());
            assertEquals("someThumbnailUrl", taxon.getThumbnailUrl());

            taxon = taxonService.getOrCreateTaxon(new TaxonImpl("bla bla boo"));
            assertEquals("bla bla", taxon.getName());
            assertEquals("a path", taxon.getPath());
            assertEquals("anExternalId", taxon.getExternalId());

            taxon = taxonService.getOrCreateTaxon(new TaxonImpl("boo bla"));
            assertEquals("boo bla", taxon.getName());
            assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
            assertNull(taxon.getPath());
        }
    }

    @Test
    public void indexResolvedOnly() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = createTaxonService(tx, new PropertyEnricherNoop());
            Taxon unresolvedTaxon = index.getOrCreateTaxon(new TaxonImpl("not resolved"));
            assertNotNull(unresolvedTaxon);
            assertFalse(TaxonUtil.isResolved(unresolvedTaxon));

            index.setIndexResolvedTaxaOnly(true);
            Taxon taxon = new TaxonImpl("no resolving either", null);
            assertFalse(TaxonUtil.isResolved(taxon));
            assertNull(index.getOrCreateTaxon(taxon));
        }
    }

    @Test
    public void createTaxonWithExplicitRanks() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = createTaxonService(tx, new PropertyEnricherNoop());
            Taxon taxon1 = new TaxonImpl("foo", "foo:123");
            taxon1.setPath("a kingdom name | a phylum name | boo name | a class name | an order name | a family name | a genus name | a species name");
            taxon1.setPathIds("a kingdom id | a phylum id | boo id | a class id | an order id | a family id | a genus id | a species id");
            taxon1.setPathNames("kingdom | phylum | boo | class | order | family | genus | species");
            Taxon taxon = index.getOrCreateTaxon(taxon1);

//        assertThat(propertyOf(taxon, "kingdomName"), is("a kingdom name"));
//        assertThat(propertyOf(taxon, "kingdomId"), is("a kingdom id"));
//        assertThat(propertyOf(taxon, "phylumName"), is("a phylum name"));
//        assertThat(propertyOf(taxon, "phylumId"), is("a phylum id"));
//
//        assertThat(propertyOf(taxon, "orderName"), is("an order name"));
//        assertThat(propertyOf(taxon, "orderId"), is("an order id"));
//
//        assertThat(propertyOf(taxon, "className"), is("a class name"));
//        assertThat(propertyOf(taxon, "classId"), is("a class id"));
//        assertThat(propertyOf(taxon, "familyName"), is("a family name"));
//        assertThat(propertyOf(taxon, "familyId"), is("a family id"));
//        assertThat(propertyOf(taxon, "genusName"), is("a genus name"));
//        assertThat(propertyOf(taxon, "genusId"), is("a genus id"));
//        assertThat(propertyOf(taxon, "speciesName"), is("a species name"));
//        assertThat(propertyOf(taxon, "speciesId"), is("a species id"));

        }


    }

    private static TaxonIndexFactory createTaxonService() {
        PropertyEnricherSingle enricher = new PropertyEnricherSingle() {
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
        };
        return new TaxonIndexFactory() {
            @Override
            public ResolvingTaxonIndex create(Transaction tx) {
                return createTaxonService(tx, enricher);
            }
        };
    }

    private static ResolvingTaxonIndex createTaxonService(Transaction tx, PropertyEnricher enricher) {
        return new ResolvingTaxonIndexImpl(enricher, tx);
    }

    @Ignore
    @Test
    public final void synonymsAddedToIndexOnce() throws NodeFactoryException {
        PropertyEnricherSingle enricher = new PropertyEnricherSingle() {
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
        };

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = createTaxonService(tx, enricher);

            Taxon taxon2 = new TaxonImpl("not pref", null);
            taxon2.setPath("need | some | path");
            Taxon first = index.getOrCreateTaxon(taxon2);
            assertThat(first.getName(), is("preferred"));
            assertThat(first.getPath(), is("one | two | three"));
            assertThat(first.getPathIds(), is("1 | 2 | 3"));

            Taxon taxon1 = new TaxonImpl("not pref", null);
            taxon1.setPath("need | some | path");
            Taxon second = index.getOrCreateTaxon(taxon1);
            assertThat(((TaxonNode) second).getNodeID(), is(((TaxonNode) first).getNodeID()));

            Taxon third = index.getOrCreateTaxon(new TaxonImpl("not pref"));
            assertThat(((TaxonNode) third).getNodeID(), is(((TaxonNode) first).getNodeID()));

            TaxonNode foundTaxon = (TaxonNode) index.findTaxonByName("not pref");
            assertThat(foundTaxon.getNodeID(), is(((TaxonNode) first).getNodeID()));

            foundTaxon = (TaxonNode) index.findTaxonByName("preferred");
            assertThat(foundTaxon.getNodeID(), is(((TaxonNode) first).getNodeID()));
        }
    }

    @Test
    public final void doNotMatchHomonyms() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonService = createTaxonService(tx, new PropertyEnricherNoop());

            Taxon taxon2 = new TaxonImpl("some name", null);
            taxon2.setPath("one | two | three | some name");
            taxon2.setPathNames("kingdom | family | genus | species");

            Taxon first = taxonService.getOrCreateTaxon(taxon2);

            assertThat(first.getName(), is("some name"));
            assertThat(first.getPath(), is("one | two | three | some name"));

            Taxon taxon1 = new TaxonImpl("some name", null);
            taxon1.setPath("four | five | six | some name");
            taxon1.setPathNames("kingdom | family | genus | species");

            Taxon second = taxonService.getOrCreateTaxon(taxon1);

            assertThat(second.getName(), is("some name"));
            assertThat(second.getPath(), is("four | five | six | some name"));

        }


    }
    @Test
    public final void doNotMatchHomonymsExceptForMatchingByExternalId() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonService = createTaxonService(tx, new PropertyEnricherNoop());

            String uuid = UUID.randomUUID().toString();
            Taxon taxon2 = new TaxonImpl("some name", uuid);
            taxon2.setPath("one | two | three | some name");
            taxon2.setPathNames("kingdom | family | genus | species");

            Taxon first = taxonService.getOrCreateTaxon(taxon2);

            assertThat(first.getName(), is("some name"));
            assertThat(first.getPath(), is("one | two | three | some name"));

            Taxon taxon1 = new TaxonImpl("some name", uuid);
            taxon1.setPath("four | five | six | some name");
            taxon1.setPathNames("kingdom | family | genus | species");

            Taxon second = taxonService.getOrCreateTaxon(taxon1);

            assertThat(second.getName(), is("some name"));
            assertThat(second.getPath(), is("one | two | three | some name"));

        }


    }

    @Test
    public final void labelUnambiguousMatchesByPath() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = createTaxonService(tx, getAnuraEnricher());

            TaxonImpl anura = new TaxonImpl("Anura", null);
            anura.setPath("four | five | six | some name");
            anura.setPathNames("kingdom | family | genus | species");

            Taxon first = index.getOrCreateTaxon(anura);
            assertThat(first.getName(), is("Anura"));
            assertThat(first.getExternalId(), is("frogs:1"));

            Taxon found = index.findTaxonByName("Anura");
            assertThat(found.getName(), is("Anura"));
            assertThat(found.getExternalId(), is("frogs:1"));
        }

    }

    @Test
    public final void labelUnambiguousMatchesById() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = createTaxonService(tx, getAnuraEnricher());

            TaxonImpl anura = new TaxonImpl("Anura", "frogs:1");

            Taxon first = index.getOrCreateTaxon(anura);
            assertThat(first.getName(), is("Anura"));
            assertThat(first.getExternalId(), is("frogs:1"));
            assertThat(first.getPath(), is("four | five | six | some name"));

            Taxon found = index.findTaxonByName("Anura");
            assertThat(found.getName(), is("Anura"));
            assertThat(found.getExternalId(), is("frogs:1"));
        }

    }

    @Test
    public final void shortName() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = createTaxonService(tx, getIaHits());

            TaxonImpl ia = new TaxonImpl("Ia", null);

            Taxon first = index.getOrCreateTaxon(ia);
            assertThat(first.getName(), is("Ia"));
        }
    }

    @Test(expected = NodeFactoryException.class)
    public final void unlikelyAndShortName() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex index = getTaxonIndexFactory().create(tx);
            try {
                index.getOrCreateTaxon(new TaxonImpl("I_", null));
            } catch (NodeFactoryException ex) {
                assertThat(ex.getMessage(), is("taxon name [I_] is a short and unlikely taxonomic name, and no externalId is provided"));
                throw ex;
            }
        }
    }

    private static PropertyEnricher getAnuraEnricher() {
        return new PropertyEnricher() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return enrichAllMatches(properties).get(0);
            }

            @Override
            public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon1 = new TaxonImpl("Anura", "frogs:1");
                taxon1.setPath("four | five | six | some name");
                taxon1.setPathNames("kingdom | family | genus | species");

                Taxon taxon2 = new TaxonImpl("Anura", "mollusk:1");
                taxon2.setPath("one | two | three | some name");
                taxon2.setPathNames("kingdom | family | genus | species");
                return Arrays.asList(TaxonUtil.taxonToMap(taxon1), TaxonUtil.taxonToMap(taxon2));
            }

            @Override
            public void shutdown() {

            }
        };
    }

    private static PropertyEnricher getIaHits() {
        return new PropertyEnricher() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return enrichAllMatches(properties).get(0);
            }

            @Override
            public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
                Taxon taxon1 = new TaxonImpl("Ia", null);
                taxon1.setPath("four | five | six | some name");
                taxon1.setPathNames("kingdom | family | genus | species");

                return Collections.singletonList(TaxonUtil.taxonToMap(taxon1));
            }

            @Override
            public void shutdown() {

            }
        };
    }

    @Test
    public final void createTaxon() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("bla bla", null);
        taxon1.setPath(null);
        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon taxon = getTaxonIndexFactory().create(tx).getOrCreateTaxon(taxon1);
            assertThat(taxon, is(notNullValue()));
            assertEquals("bla bla", taxon.getName());
        }
    }

    @Test
    public final void createNullTaxon() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(null, "EOL:1234");
        taxon1.setPath(null);
        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon taxon = getTaxonIndexFactory().create(tx).getOrCreateTaxon(taxon1);
            assertThat(taxon, is(notNullValue()));
            assertEquals("no name", taxon.getName());
        }
    }

    @Test
    public final void getOrCreateNullTaxon() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon taxon = getTaxonIndexFactory().create(tx).getOrCreateTaxon(null);
            assertThat(taxon, is(CoreMatchers.nullValue()));
        }
    }

    @Test
    public final void createTaxonExternalIdIndex() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(null, "foo:123");
        taxon1.setPath(null);
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            Taxon taxon = taxonIndex.getOrCreateTaxon(taxon1);
            assertThat(taxon, is(notNullValue()));
            assertThat(taxonIndex.findTaxonById("foo:123", taxon), is(notNullValue()));
        }
    }


//    static Object propertyOf(Node taxon, String propertyName) {
//

    /// /        return taxon.getUnderlyingNode().getProperty(propertyName);
//    }
    @Test
    public final void doNotIndexMagicValuesTaxon() throws NodeFactoryException {
        assertNotIndexed(PropertyAndValueDictionary.NO_NAME);
        assertNotIndexed(PropertyAndValueDictionary.NO_MATCH);
        assertNotIndexed(PropertyAndValueDictionary.AMBIGUOUS_MATCH);
    }

    private void assertNotIndexed(String magicValue) throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl(magicValue, null);
        taxon1.setPath(null);
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            Taxon taxon = taxonIndex.getOrCreateTaxon(taxon1);
            assertThat(taxon, is(notNullValue()));
            assertThat(taxonIndex.findTaxonByName(magicValue), is(CoreMatchers.nullValue()));
            tx.commit();
        }
    }

    @Test
    public final void findCloseMatch() throws NodeFactoryException {
//        factory.getOrCreateTaxon(new TaxonImpl("Homo sapiens"));
//        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonName("Homo sapiens");
//        assertThat(hits.hasNext(), is(true));
//        hits.close();
//        hits = taxonService.findCloseMatchesForTaxonName("Homo saliens");
//        assertThat(hits.hasNext(), is(true));
//        hits = taxonService.findCloseMatchesForTaxonName("Homo");
//        assertThat(hits.hasNext(), is(true));
//        hits = taxonService.findCloseMatchesForTaxonName("homo sa");
//        assertThat(hits.hasNext(), is(true));
    }

    @Test
    public final void findNoMatchNoName() throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            taxonIndex.getOrCreateTaxon(new TaxonImpl("some name", PropertyAndValueDictionary.NO_MATCH));
            assertNull(taxonIndex.findTaxonById(PropertyAndValueDictionary.NO_MATCH));
            taxonIndex.getOrCreateTaxon(new TaxonImpl(PropertyAndValueDictionary.NO_NAME));
            assertNull(taxonIndex.findTaxonByName(PropertyAndValueDictionary.NO_MATCH));
            assertNull(taxonIndex.findTaxonByName(PropertyAndValueDictionary.NO_NAME));
        }
    }

    @Ignore("disable homonym detection methods for now; related to https://github.com/globalbioticinteractions/globalbioticinteractions/issues/871")
    @Test
    public final void indexTwoHomonymsSeparately() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("some name 4567", null);
        taxon1.setPath("one | two | three | some name 4567");
        taxon1.setPathNames("kingdom | family | genus | species");

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            assertThat(taxonIndex.getOrCreateTaxon(taxon1), is(CoreMatchers.nullValue()));

            Taxon taxon = taxonIndex.getOrCreateTaxon(taxon1);
            assertThat(taxon, is(notNullValue()));

            assertThat(taxonIndex.getOrCreateTaxon(taxon1), is(not(CoreMatchers.nullValue())));
            assertThat(taxonIndex.findTaxonByName("some name 4567"), is(notNullValue()));

            Taxon taxon2 = new TaxonImpl("some name 4567", null);
            taxon2.setPath("four | five | six | some name 4567");
            taxon2.setPathNames("kingdom | family | genus | species");

            assertThat(taxonIndex.getOrCreateTaxon(taxon2), is(CoreMatchers.nullValue()));
            tx.commit();
        }
    }

    @Ignore("disable homonym detection methods for now; related to https://github.com/globalbioticinteractions/globalbioticinteractions/issues/871")
    @Test
    public final void indexTwoHomonymsSeparately2() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("some name", "foo:123");
        taxon1.setPath("seven | eight | nine | some name");
        taxon1.setPathNames("kingdom | family | genus | species");

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            Taxon taxon = taxonIndex.getOrCreateTaxon(taxon1);
            assertThat(taxon.getPath(), is("seven | eight | nine | some name"));
            assertThat(taxonIndex.getOrCreateTaxon(taxon1), is(not(CoreMatchers.nullValue())));

            assertThat(taxonIndex.findTaxonById("foo:123", taxon), is(notNullValue()));

            Taxon taxon2 = new TaxonImpl("some name", "foo:123");
            taxon2.setPath("ten | eleven | twelve | some name");
            taxon2.setPathNames("kingdom | family | genus | species");
            Taxon homonym = taxonIndex.getOrCreateTaxon(taxon2);
            //assertThat(homonym.getNodeID(), is(not(taxon.getNodeID())));

            Taxon taxon3 = new TaxonImpl("some name");

            assertThat(taxonIndex.getOrCreateTaxon(taxon3).getPath(), is("seven | eight | nine | some name"));
            tx.commit();
        }
    }

    @Test
    public final void indexHomonymExplicitly() throws NodeFactoryException {
        String externalId = null;
        Taxon taxon1 = new TaxonImpl("some name", externalId);
        taxon1.setPath("one | two | three | some name");
        taxon1.setPathNames("kingdom | family | genus | species");
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            assertThat(taxonIndex.getOrCreateTaxon(taxon1), is(notNullValue()));

            Taxon taxon2 = new TaxonImpl("some name", externalId);
            taxon2.setPath("some | other | path | for | some name");
            taxon2.setPathNames("kingdom | phylum | family | genus | species");
            Taxon suspectedHomonym = taxonIndex.getOrCreateTaxon(taxon2);
            assertThat(suspectedHomonym, is(not(CoreMatchers.nullValue())));
            assertThat(suspectedHomonym.getPath(), is("some | other | path | for | some name"));
        }
    }

    @Test
    public final void indexHomonymExplicitlyExceptWhenMatchingOnExternalId() throws NodeFactoryException {
        String externalId = "some:id";
        Taxon taxon1 = new TaxonImpl("some name", externalId);
        taxon1.setPath("one | two | three | some name");
        taxon1.setPathNames("kingdom | family | genus | species");
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            assertThat(taxonIndex.getOrCreateTaxon(taxon1), is(notNullValue()));

            Taxon taxon2 = new TaxonImpl("some name", externalId);
            taxon2.setPath("some | other | path | for | some name");
            taxon2.setPathNames("kingdom | phylum | family | genus | species");
            Taxon suspectedHomonym = taxonIndex.getOrCreateTaxon(taxon2);
            assertThat(suspectedHomonym, is(not(CoreMatchers.nullValue())));
            assertThat(suspectedHomonym.getPath(), is("one | two | three | some name"));
        }
    }


}
