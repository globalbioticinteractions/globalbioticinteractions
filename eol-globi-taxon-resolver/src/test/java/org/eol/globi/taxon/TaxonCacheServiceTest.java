package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.TermUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TaxonCacheServiceTest {

    private static final String TAXON_MAP_TEST_RESOURCE = "/org/eol/globi/taxon/taxonMap.tsv";
    private static final String TAXON_CACHE_TEST_RESOURCE = "/org/eol/globi/taxon/taxonCache.tsv";
    private File mapdbDir;

    @Before
    public void createMapDBFolder() throws IOException {
        mapdbDir = new File("./target/mapdb" + new Random().nextLong());
    }

    @After
    public void deleteMapDBFolder() throws IOException {
        FileUtils.deleteQuietly(mapdbDir);
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Green-winged teal");
            }
        };
        final TaxonCacheService cacheService = getTaxonCacheService();
        Map<String, String> enrich = cacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
        assertThat(enrichedTaxon.getThumbnailUrl(), is("http://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
    }

    @Test
    public void matchTermByName() throws PropertyEnricherException {
        final TaxonCacheService cacheService = getTaxonCacheService();

        AtomicBoolean matched = new AtomicBoolean(false);
        cacheService.findTerms(TermUtil.toNamesToTerms(Collections.singletonList("Green-winged teal")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term term, Taxon enrichedTaxon, NameType nameType) {
                assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
                assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
                assertThat(enrichedTaxon.getThumbnailUrl(), is("http://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
                matched.set(true);
            }
        });
        assertTrue(matched.get());
    }

    @Test
    public void matchTermByNameLowerCase() throws PropertyEnricherException {
        final TaxonCacheService cacheService = getTaxonCacheService();

        AtomicBoolean matched = new AtomicBoolean(false);
        cacheService.findTerms(TermUtil.toNamesToTerms(Collections.singletonList("green-winged teal")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon enrichedTaxon, NameType nameType) {
                assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
                assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
                assertThat(enrichedTaxon.getThumbnailUrl(), is("http://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
                matched.set(true);
            }
        });
        assertTrue(matched.get());
    }

    @Test
    public void matchTermByNameFirstLine() throws PropertyEnricherException {
        final TaxonCacheService cacheService = new TaxonCacheService("/org/eol/globi/taxon/taxonCacheNoHeader.tsv", TAXON_MAP_TEST_RESOURCE);
        cacheService.setCacheDir(mapdbDir);

        AtomicBoolean matched = new AtomicBoolean(false);
        cacheService.findTerms(TermUtil.toNamesToTerms(Collections.singletonList("EOL:1276240")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon enrichedTaxon, NameType nameType) {
                assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
                assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
                matched.set(true);
            }
        });
        assertTrue(matched.get());
    }

    @Test
    public void matchCalyptraWithExtraWhitespace() throws PropertyEnricherException {
        String name = "Calyptra  thalictri";
        assertMatchFor(name);
    }

    @Test
    public void matchCalyptraWithNoExtraWhitespace() throws PropertyEnricherException {
        String name = "Calyptra thalictri";
        assertMatchFor(name);
    }

    private void assertMatchFor(String name) throws PropertyEnricherException {
        // see https://github.com/seltmann/vampire-moth-globi/issues/3
        String termCache = "/org/eol/globi/taxon/taxonCacheCalyptra.tsv";
        String termMap = "/org/eol/globi/taxon/taxonMapCalyptra.tsv";
        final TaxonCacheService cacheService = new TaxonCacheService(termCache, termMap);
        cacheService.setCacheDir(mapdbDir);

        AtomicBoolean matched = new AtomicBoolean(false);
        cacheService.findTerms(TermUtil.toNamesToTerms(Collections.singletonList(name)), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon enrichedTaxon, NameType nameType) {
                if (!matched.get()) {
                    assertThat(enrichedTaxon.getExternalId(), is("EOL:545931"));
                    assertThat(enrichedTaxon.getName(), is("Calyptra thalictri"));
                    matched.set(true);
                }
            }
        });
        assertTrue(matched.get());
    }

    @Test
    public void noMatchExplicit() throws PropertyEnricherException {
        final TaxonCacheService cacheService = new TaxonCacheService("/org/eol/globi/taxon/taxonCacheNoHeader.tsv", TAXON_MAP_TEST_RESOURCE);
        cacheService.setCacheDir(mapdbDir);

        AtomicBoolean matched = new AtomicBoolean(false);
        cacheService.findTerms(TermUtil.toNamesToTerms(Collections.singletonList("foo:bar")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term term, Taxon enrichedTaxon, NameType nameType) {
                assertThat(nameType, is(NameType.NONE));
                matched.set(true);
            }
        });
        assertTrue(matched.get());
    }

    @Test
    public void noMatchByTermExplicit() throws PropertyEnricherException {
        final TaxonCacheService cacheService = new TaxonCacheService("/org/eol/globi/taxon/taxonCacheNoHeader.tsv", TAXON_MAP_TEST_RESOURCE);
        cacheService.setCacheDir(mapdbDir);

        AtomicBoolean matched = new AtomicBoolean(false);
        cacheService.findTerms(Collections.singletonList(new TermImpl("foo:bar", null)), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon enrichedTaxon, NameType nameType) {
                assertThat(nameType, is(NameType.NONE));
                matched.set(true);
            }
        });
        assertTrue(matched.get());
    }

    @Test
    public void enrichByNameMissingThumbnail() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Acteocina inculcata");
            }
        };
        final TaxonCacheService cacheService = getTaxonCacheService();
        Map<String, String> enrich = cacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Acteocina inculta"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:455065"));
        assertThat(enrichedTaxon.getCommonNames(), is("rude barrel-bubble @en |"));
        assertThat(enrichedTaxon.getThumbnailUrl(), is(""));
    }

    @Test
    public void enrichByNameInconsistentCasing() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "acteocina inculcata");
            }
        };
        final TaxonCacheService cacheService = getTaxonCacheService();
        Map<String, String> enrich = cacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Acteocina inculta"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:455065"));
        assertThat(enrichedTaxon.getCommonNames(), is("rude barrel-bubble @en |"));
        assertThat(enrichedTaxon.getThumbnailUrl(), is(""));
    }

    private TaxonCacheService getTaxonCacheService() {
        final TaxonCacheService cacheService = new TaxonCacheService(TAXON_CACHE_TEST_RESOURCE, TAXON_MAP_TEST_RESOURCE);
        cacheService.setCacheDir(mapdbDir);
        return cacheService;
    }

    @Test
    public void enrichPassThrough() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "some name");
                put(PropertyAndValueDictionary.EXTERNAL_ID, "some cached externalId");
            }
        };
        Map<String, String> enrich = getTaxonCacheService().enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("some name"));
        assertThat(enrichedTaxon.getExternalId(), is("some cached externalId"));
    }

    @Test
    public void enrichPassThroughNoMatch() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, PropertyAndValueDictionary.NO_MATCH);
            }
        };
        Map<String, String> enrich = getTaxonCacheService().enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(enrichedTaxon.getExternalId(), is(nullValue()));
    }

    @Test
    public void enrichById() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:1276240");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByResolvedId2() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:9606");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Homo sapiens"));
        assertThat(enrichedTaxon.getExternalId(), is("NCBI:9606"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByUnlikelyId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "1276240");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is(nullValue()));
        assertThat(enrichedTaxon.getExternalId(), is("1276240"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByIdLowerCase() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "eol:1276240");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByIdName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Gadus morhua");
                put(PropertyAndValueDictionary.EXTERNAL_ID, "FBC:FB:SPECCODE:69");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Gadus morhua"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1234"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByNameAndId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Anas crecca carolinensis");
                put(PropertyAndValueDictionary.EXTERNAL_ID, "SOME:123");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
        assertThat(TaxonUtil.isResolved(enrichedTaxon), is(true));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByIdZikaNoCachedPath() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:541190");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is(nullValue()));
        assertThat(enrichedTaxon.getPath(), is(nullValue()));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:541190"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByIdGzip() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:1276240");
            }
        };
        Map<String, String> enrich = getTaxonCacheService().enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Anas crecca carolinensis"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1276240"));
    }


    @Test
    public void enrichByResolvedId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:327955");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Homo sapiens"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:327955"));
        taxonCacheService.shutdown();
    }

    @Test
    public void enrichByResolvedName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Homo sapiens");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Homo sapiens"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:327955"));
        taxonCacheService.shutdown();
    }

    @Test
    public void resolveSecondary() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Felis catus");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Felis catus"));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:1037781"));
        taxonCacheService.shutdown();
    }

    @Test
    public void resolveWithoutDuplicates() throws PropertyEnricherException {
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        List<Taxon> taxa = new ArrayList<>();
        taxonCacheService.findTerms(Arrays.asList(new TermImpl(null, "Felis catus")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon taxon, NameType nameType) {
                taxa.add(taxon);
            }
        });


        assertThat(taxa.size(), is(1));
        taxonCacheService.shutdown();
    }

    @Test
    public void resolveWithCrossDomainMapping() throws PropertyEnricherException {
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        List<Taxon> taxa = new ArrayList<>();
        taxonCacheService.findTerms(Collections.singletonList(new TermImpl("EOL:327955", null)), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon taxon, NameType nameType) {
                taxa.add(taxon);
            }
        });

        assertThat(taxa.size(), is(2));

        assertThat(taxa.get(0).getExternalId(), is("EOL:327955"));
        assertThat(taxa.get(0).getExternalUrl(), is("http://eol.org/pages/327955"));
        assertThat(taxa.get(1).getExternalId(), is("NCBI:9606"));
        assertThat(taxa.get(1).getExternalUrl(), not(is("http://eol.org/pages/327955")));

        taxonCacheService.shutdown();
    }

    @Test
    public void resolveUnresolvedOnly() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "Canis lupus dingo");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is("Canis lupus dingo"));
        assertThat(enrichedTaxon.getExternalId(), is(nullValue()));
        taxonCacheService.shutdown();
    }

    @Test
    public void resolveZika() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:541190");
            }
        };
        final TaxonCacheService taxonCacheService = getTaxonCacheService();
        Map<String, String> enrich = taxonCacheService.enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrich);
        assertThat(enrichedTaxon.getName(), is(nullValue()));
        assertThat(enrichedTaxon.getExternalId(), is("EOL:541190"));
        taxonCacheService.shutdown();
    }


    @Test
    public void resolveWithSameIdDifferentName() throws PropertyEnricherException {
        final TaxonCacheService cacheService = new TaxonCacheService(
                "/org/eol/globi/taxon/taxonCacheRhusSylvestris.tsv",
                "/org/eol/globi/taxon/taxonMapRhusSylvestris.tsv");
        cacheService.setMaxTaxonLinks(125);
        cacheService.setCacheDir(mapdbDir);
        assertRhus(cacheService, new TermImpl("", "Rhus sylvestris"));
        assertRhus(cacheService, new TermImpl("EOL:2888778", ""));
        assertRhus(cacheService, new TermImpl("EOL:2888778", "Rhus sylvestris"));
    }

    private void assertRhus(TaxonCacheService cacheService, TermImpl searchTerm) throws PropertyEnricherException {
        Set<String> listIds = new HashSet<>();
        Set<String> listNames = new HashSet<>();

        cacheService.findTerms(Collections.singletonList(searchTerm), new TermMatchListener() {

            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon taxon, NameType nameType) {
                listIds.add(taxon.getExternalId());
                listNames.add(taxon.getName());
            }
        });

        assertThat(listNames, not(hasItem("Rhus sylvestris")));
        assertThat(listNames, hasItem("Toxicodendron sylvestre"));
        assertThat(listIds, contains("GBIF:4928886", "NCBI:269722", "OTT:301953", "WD:Q1193796"));
    }

    @Test
    public void resolveWithMultipleSchemes() throws PropertyEnricherException {
        final TaxonCacheService cacheService = new TaxonCacheService(
                "/org/eol/globi/taxon/taxonCacheHomoSapiens.tsv",
                "/org/eol/globi/taxon/taxonMapHomoSapiens.tsv");
        cacheService.setMaxTaxonLinks(125);
        cacheService.setCacheDir(mapdbDir);
        Set<String> listIds = new TreeSet<>();
        Set<String> listNames = new HashSet<>();
        cacheService.findTerms(Collections.singletonList(new TermImpl("", "Homo sapiens")), new TermMatchListener() {

            @Override
            public void foundTaxonForName(Long nodeId, Term name, Taxon taxon, NameType nameType) {
                listIds.add(taxon.getExternalId());
                listNames.add(taxon.getName());
            }
        });

        assertThat(listNames, hasItem("Homo sapiens"));
        assertThat(listIds, contains("EOL:327955", "GBIF:2436436",
                "INAT_TAXON:43584", "IRMNG:10857762",
                "NCBI:741158", "OTT:933436", "WD:Q15978631"));


    }

    @Test
    public void invalidateAll() throws PropertyEnricherException {
        TermResource<Taxon> termCache = new TermResource<Taxon>() {

            @Override
            public String getResource() {
                return TAXON_CACHE_TEST_RESOURCE;
            }

            @Override
            public Function<String, Taxon> getParser() {
                return null;
            }

            @Override
            public Predicate<String> getValidator() {
                return s -> false;
            }
        };

        TermResource<Triple<Taxon, NameType, Taxon>> termMap = new TermResource<Triple<Taxon, NameType, Taxon>>() {

            @Override
            public String getResource() {
                return TAXON_MAP_TEST_RESOURCE;
            }

            @Override
            public Function<String, Triple<Taxon, NameType, Taxon>> getParser() {
                return str -> {
                    throw new RuntimeException("kaboom!");
                };
            }

            @Override
            public Predicate<String> getValidator() {
                return s -> false;
            }
        };
        final TaxonCacheService cacheService = new TaxonCacheService(termCache, termMap);
        cacheService.setCacheDir(mapdbDir);

        cacheService.findTerms(Collections.singletonList(new TermImpl("EOL:1276240", null)),
                (nodeId, name, taxon, nameType) -> assertThat(nameType, is(NameType.NONE)));
    }
}