package org.eol.globi.taxon;

import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnrichmentFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.PropertyAndValueDictionary.COMMON_NAMES;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_IDS;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_NAMES;
import static org.eol.globi.domain.PropertyAndValueDictionary.RANK;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

// test fail randonmly because EOL API is producing intermittent errors, see
public class EOLServiceIT {

    private final EOLService eolService = new EOLService();

    @Before
    public void init() {
        eolService.setFilter(new PropertyEnrichmentFilterWithPathOnly());
    }

    @Test
    public void lookupByName() throws PropertyEnricherException {
        assertThat(lookupPageIdByName("Actinopterygii"), is("EOL:1905"));
        assertThat(lookupPageIdByName("Catfish"), is("EOL:5083"));
        assertThat(lookupPageIdByName("Hygrocybe pratensis var. pallida"), is("EOL:6676627"));

        assertThat(lookupPageIdByName("Homo sapiens"), is("EOL:327955"));
        assertThat(lookupPageIdByName("Puccinia caricina var. ribesii-pendulae"), is("EOL:6776658"));
        assertThat(lookupPageIdByName("Hesperocharis paranensis"), is("EOL:176594"));

        assertThat(lookupPageIdByName("Dead roots"), is(nullValue()));
        assertThat(lookupPageIdByName("Prunella (Bot)"), is("EOL:70879"));
        //assertThat(lookupPageIdByScientificName("Prunella (Bird)"), is("EOL:77930"));
        assertThat(lookupPageIdByName("Pseudobaeospora dichroa"), is("EOL:1001400"));
    }

    @Test
    public void taxonFilter() throws PropertyEnricherException {
        final HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        EOLService eolService = this.eolService;
        eolService.setFilter(new PropertyEnrichmentFilter() {
            @Override
            public boolean shouldReject(Map<String, String> properties) {
                return true;
            }
        });
        Map<String, String> props = eolService.enrich(properties);
        assertThat(props.get(EXTERNAL_ID), is(nullValue()));

        eolService.setFilter(new PropertyEnrichmentFilter() {
            @Override
            public boolean shouldReject(Map<String, String> properties) {
                return false;
            }
        });
        props = eolService.enrich(properties);
        assertThat(props.get(EXTERNAL_ID), is(notNullValue()));
    }

    @Test
    public void taxonPathIncludesNotAssigned() throws PropertyEnricherException {
        final HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Armandia agilis");
        Map<String, String> props = eolService.enrich(properties);
        assertThat(props.get(EXTERNAL_ID), is(notNullValue()));
        assertThat(props.get(PATH), is(notNullValue()));
        assertThat(props.get(PATH), not(containsString("Not")));
        assertThat(props.get(PATH_IDS), is(notNullValue()));
        assertThat(props.get(PATH_IDS), not(containsString("EOL:16833350")));
        assertThat(props.get(PATH_NAMES), is(notNullValue()));
        assertThat(props.get(PATH_NAMES), not(containsString("order")));
    }

    @Test
    public void nonTaxonPage() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "EOL:29725463");
        Map<String, String> enrich = new EOLService().enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is(nullValue()));
    }

    @Test
    public void bibioFerruginatus() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(EXTERNAL_ID, "EOL:756665");
        Map<String, String> enrich = new EOLService().enrich(properties);
        assertThat(enrich.get(NAME), is("Bibio ferruginatus"));
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:756665"));
    }

    @Test
    public void zikaVirus() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "EOL:541190");
        Map<String, String> enrich = new EOLService().enrich(properties);
        assertThat(enrich.get(PATH_IDS), containsString("EOL:541190"));
        assertThat(enrich.get(PATH), containsString("Zika virus"));
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:541190"));
    }


    @Ignore(value = "not quite sure about classification because it points to one of many known Algae classifications")
    @Test
    public void algae() throws PropertyEnricherException {
        assertThat(lookupPageIdByName("Algae"), is("EOL:3353"));
    }

    @Test
    public void greySmoothhound() throws PropertyEnricherException {
        assertThat(lookupPageIdByName("Grey Smoothhound"), is(nullValue()));
    }

    @Test
    public void benedenia() throws PropertyEnricherException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/307#issuecomment-322836016
        assertThat(lookupPageIdByName("BENEDENIA"), is(not("EOL:7660")));
    }

    @Test
    public void boron() throws PropertyEnricherException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/307#issuecomment-322836016
        assertThat(lookupPageIdByName("Boron deficiency"), is(nullValue()));
        assertThat(lookupPageIdByName("Boron"), is(not("EOL:213908")));
    }

    @Test
    public void lookupBySquatLobster() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Squat lobster");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:315099"));
        assertThat(enrich.get(NAME), is("Munidopsis albatrossae"));
        assertThat(enrich.get(RANK), is("Species"));
    }

    @Test
    public void lookupAlfalfaMosaicVirus() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Alfalfa mosaic virus");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(NAME), is("Alfalfa mosaic virus"));
        assertThat(enrich.get(PATH), containsString("Alfalfa mosaic virus"));
        assertThat(enrich.get(RANK), is("Species"));
    }

    @Ignore("for some reason UTF8 characters do not result in exact matches")
    @Test
    public void lookupByAcheloussprinicarpusUTF8() throws PropertyEnricherException {
        for (String name : new String[]{"Acheloüs spinicarpus", "Achelous spinicarpus"}) {
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(NAME, name);
            Map<String, String> enrich = eolService.enrich(properties);
            assertThat(enrich.get(EXTERNAL_ID), is("EOL:343000"));
            assertThat(enrich.get(NAME), is("Acheloüs spinicarpus"));
            assertThat(enrich.get(RANK), is("Species"));
        }
    }

    @Test
    public void lookupGlasswortFormerlyPickleweed() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "glasswort");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:61812"));
        assertThat(enrich.get(NAME), is("Salicornia"));
        assertThat(enrich.get(RANK), is("Genus"));
        assertThat(enrich.get(PATH), is("Plantae | Tracheophyta | Magnoliopsida | Caryophyllales | Chenopodiaceae | Salicornia"));
        assertThat(enrich.get(PATH_NAMES), is("kingdom | phylum | class | order | family | genus"));

    }

    @Test
    @Ignore("suspected re-use of \"stable\" page id, see https://github.com/jhpoelen/eol-globi-data/issues/268")
    public void lookupCalyptridium() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "EOL:754947");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:754947"));
        assertThat(enrich.get(PATH), is("Cellular organisms | Eukaryota | Viridiplantae | Streptophyta | Streptophytina | Embryophyta | Tracheophyta | Euphyllophyta | Spermatophyta | Magnoliophyta | Mesangiospermae | Eudicotyledons | Gunneridae | Pentapetalae | Caryophyllales | Cactineae | Montiaceae | Calyptridium"));
        assertThat(enrich.get(NAME), is("Calyptridium"));
        assertThat(enrich.get(RANK), is("Genus"));
        assertThat(enrich.get(PATH_NAMES), is(" | superkingdom | kingdom | phylum |  |  |  |  |  |  |  |  |  |  | order | suborder | family | genus"));
        assertThat(enrich.get(PATH_IDS), is("EOL:6061725 | EOL:2908256 | EOL:8654492 | EOL:11823577 | EOL:11824138 | EOL:2913521 | EOL:4077 | EOL:11830053 | EOL:6152932 | EOL:282 | EOL:39835629 | EOL:39865587 | EOL:39868843 | EOL:39868886 | EOL:4223 | EOL:21203680 | EOL:6360216 | EOL:2500577"));
    }

    @Test
    public void lookupPyrguscirsii() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Pyrgus cirsii");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:186021"));
        assertThat(enrich.get(NAME), is("Pyrgus cirsii"));
        assertThat(enrich.get(RANK), is("Species"));
        assertThat(enrich.get(PATH), containsString("Insecta"));
        assertThat(enrich.get(PATH), containsString("Pyrgus cirsii"));
        assertThat(enrich.get(PATH_IDS), containsString("EOL:344 "));
        assertThat(enrich.get(PATH_IDS), containsString("EOL:186021"));
    }

    @Test
    public void sphyrnaMokarran() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Sphyrna mokarran");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PATH), is("Animalia | Chordata | Elasmobranchii | Carcharhiniformes | Sphyrnidae | Sphyrna | Sphyrna mokarran"));
        assertThat(enrich.get(RANK), is("Species"));
    }

    @Test
    public void suspensionFeeders() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Other suspension feeders");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is(nullValue()));
        assertThat(enrich.get(PATH), is(nullValue()));
    }

    @Test
    public void lookupPickleweedAlreadyEnriched() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "EOL:61812");
        properties.put(NAME, "a name");
        properties.put(RANK, "a rank");
        properties.put(PATH, "a path");
        properties.put(COMMON_NAMES, "a common name");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:61812"));
        assertThat(enrich.get(NAME), is("a name"));
        assertThat(enrich.get(RANK), is("a rank"));
        assertThat(enrich.get(PATH), is("a path"));
    }

    @Test
    public void lookupHake() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Hake");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:206057"));
        assertThat(enrich.get(NAME), is("Urophycis cirrata"));
    }

    @Test
    public void lookupSupportedByNonEOLId() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        String nonEOLId = TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY + "1235";
        properties.put(EXTERNAL_ID, nonEOLId);
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is(nonEOLId));
        assertThat(enrich.get(NAME), is("Homo sapiens"));
        assertThat(enrich.get(PATH), is(nullValue()));
    }

    /**
     * Regarding multiple symbol in taxonomic names:
     * <p/>
     * From: 	xxx
     * Subject: 	RE: question about taxonomy name normalization
     * Date: 	April 12, 2013 4:24:11 AM PDT
     * To: 	xxx
     * <p/>
     * And another thing!
     * <p/>
     * The "x" in hybrids, whether "Genus species1 x species2" or "Genus
     * xHybridName" is strictly not the letter "x". It's the multiply symbol:
     * HTML: &times;
     * http://www.fileformat.info/info/unicode/char/d7/index.htm
     * <p/>
     * But of course you'll equally receive it as "x".
     * <p/>
     * Malcolm
     *
     * @throws PropertyEnricherException
     */

    @Test
    public void multiplySymbolNotSupportedByEOL() throws PropertyEnricherException {
        assertThat(lookupPageIdByName("Salix cinerea \u00D7 phylicifolia"), is(nullValue()));
        assertThat(lookupPageIdByName("Salix cinerea × phylicifolia"), is(nullValue()));

    }

    @Test
    public void lookupByNameYieldsMoreThanOneMatches() throws PropertyEnricherException {
        // this species has two matches  http://eol.org/27383107 and http://eol.org/209714, first is picked
        assertThat(lookupPageIdByName("Copadichromis insularis"), is("EOL:209714"));

        // below matches both http://eol.org/4443282 and http://eol.org/310363, but first is picked
        assertThat(lookupPageIdByName("Spilogale putorius gracilis"), is("EOL:310363"));

        // etc
        assertThat(lookupPageIdByName("Crocethia alba"), is("EOL:1049518"));
        assertThat(lookupPageIdByName("Ecdyonuridae"), is("EOL:2762776"));
        assertThat(lookupPageIdByName("Catasticta hegemon"), is("EOL:173526"));
        assertThat(lookupPageIdByName("Theridion ovatum"), is("EOL:1187291"));
        assertThat(lookupPageIdByName("Cambarus propinquus"), is(nullValue()));
        assertThat(lookupPageIdByName("Vellidae"), is("EOL:644"));
        assertThat(lookupPageIdByName("Mylothris rueppellii"), is("EOL:180170"));

    }

    @Ignore("for some reason, the assumption that pageId based lookup is faster than a name->pageId based lookup does not hold (anymore)")
    @Test
    public void enrichUsingPreExistingEOLPageId() throws PropertyEnricherException {
        //warm up
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        eolService.enrich(properties);

        properties = new HashMap<String, String>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        properties.put(NAME, "Homo sapiens");
        Map<String, String> enrich = eolService.enrich(properties);
        stopWatch.stop();
        long durationFullLookup = stopWatch.getTime();
        assertThat(enrich.get(COMMON_NAMES), is(notNullValue()));
        assertThat(enrich.get(PATH), is(notNullValue()));
        assertThat(enrich.get(EXTERNAL_ID), is(notNullValue()));
        final String externalId = properties.get(EXTERNAL_ID);

        HashMap<String, String> enrichedProperties = new HashMap<String, String>() {{
            put(EXTERNAL_ID, externalId);
        }};

        stopWatch.reset();
        stopWatch.start();
        enrichedProperties.put(NAME, "Homo sapiens");
        enrich = eolService.enrich(enrichedProperties);
        stopWatch.stop();

        long durationNonPageIdLookup = stopWatch.getTime();

        assertThat("expected full lookup [" + durationFullLookup + "] ms, to be slower than partial lookup [" + durationNonPageIdLookup + "] ms",
                durationNonPageIdLookup < durationFullLookup, is(true));

        assertThat(enrich.get(COMMON_NAMES), is(notNullValue()));
        assertThat(enrich.get(PATH), is(notNullValue()));
        assertThat(enrich.get(EXTERNAL_ID), is(notNullValue()));
    }

    @Test
    public void lookupByNameYieldsNoMatches() throws PropertyEnricherException {
        assertThat(lookupPageIdByName("Clio acicula"), is(nullValue()));
        assertThat(lookupPageIdByName("Aegires oritzi"), is(nullValue()));
        assertThat(lookupPageIdByName("Fish hook"), is(nullValue()));
    }

    @Test
    public void lookupNoneEOLExternalId() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "foo:bar");
        properties.put(NAME, "Homo sapiens");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), not(is("foo:bar")));

    }

    // see https://github.com/jhpoelen/eol-globi-data/issues/77
    @Test
    public void lookupEOLExternalIdNoClassification() throws PropertyEnricherException {
        String[] externalIds = {"EOL:3821293", "EOL:3238626", "EOL:17264771", "EOL:3825406"};
        for (String externalId : externalIds) {
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(EXTERNAL_ID, externalId);
            properties.put(NAME, null);
            properties = eolService.enrich(properties);
            assertThat(properties.get(EXTERNAL_ID), is(nullValue()));
        }
    }

    @Test
    public void lookupEOLExternalId2() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "foo:bar");
        properties.put(NAME, "Prunella vulgaris");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(EXTERNAL_ID), is("EOL:579652"));

    }

    private String lookupPageIdByName(String taxonName) throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, taxonName);
        Map<String, String> enrich = eolService.enrich(properties);
        return enrich.get(EXTERNAL_ID);
    }

    @Test
    public void parsePageIdEnsureLowestIsSelected() {
        String response = pageFeedResponsePrefix();
        response += biggerPageId();
        response += smallerPageId();
        response += pageFeedResponseSuffix();

        Long actual = eolService.findSmallestPageId(response);
        assertThat(actual, is(310363L));

        response = pageFeedResponsePrefix() + smallerPageId() + biggerPageId() + pageFeedResponseSuffix();
        actual = eolService.findSmallestPageId(response);
        assertThat(actual, is(310363L));
    }

    private String pageFeedResponsePrefix() {
        return "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\">\n" +
                "  <title>Encyclopedia of Life search: </title>\n" +
                "  <link href=\"http://eol.org/api/search/1.0/Spilogale%20putorius%20gracilis\"/>\n" +
                "  <updated/>\n" +
                "  <1049518author>\n" +
                "    <name>Encyclopedia of Life</name>\n" +
                "  </author>\n" +
                "  <id>http://eol.org/api/search/1.0/Spilogale%20putorius%20gracilis</id>\n" +
                "  <os:totalResults>2</os:totalResults>\n" +
                "  <os:startIndex>1</os:startIndex>\n" +
                "  <os:itemsPerPage>30</os:itemsPerPage>\n" +
                "  <os:Query role=\"request\" searchTerms=\"\" startPage=\"\"/>\n" +
                "  <link rel=\"alternate\" href=\"http://eol.org/api/search/1.0/Spilogale%20putorius%20gracilis/\" type=\"application/atom+xml\"/>\n" +
                "  <link rel=\"first\" href=\"http://eol.org/api/search/Spilogale%20putorius%20gracilis.xml?page=1\" type=\"application/atom+xml\"/>\n" +
                "  <link rel=\"self\" href=\"http://eol.org/api/search/Spilogale%20putorius%20gracilis.xml?page=1\" type=\"application/atom+xml\"/>\n" +
                "  <link rel=\"last\" href=\"http://eol.org/api/search/Spilogale%20putorius%20gracilis.xml?page=1\" type=\"application/atom+xml\"/>\n" +
                "  <link rel=\"search\" href=\"http://eol.org/opensearchdescription.xml\" type=\"application/opensearchdescription+xml\"/>\n";
    }

    private String pageFeedResponseSuffix() {
        return "</feed>";
    }

    private String smallerPageId() {
        return "  <entry>\n" +
                "    <title>Spilogale gracilis Merriam, 1890</title>\n" +
                "    <link href=\"http://eol.org/310363?action=overview&amp;controller=taxa\"/>\n" +
                "    <id>310363</id>\n" +
                "    <updated/>\n" +
                "    <content>Spilogale putorius gracilis; Mustela putorius gracilis</content>\n" +
                "  </entry>\n";
    }

    private String biggerPageId() {
        return "  <entry>\n" +
                "    <title>Spilogale putorius gracilis</title>\n" +
                "    <link href=\"http://eol.org/4443282?action=overview&amp;controller=taxa\"/>\n" +
                "    <id>4443282</id>\n" +
                "    <updated/>\n" +
                "    <content>Spilogale putorius gracilis</content>\n" +
                "  </entry>\n";
    }

    @Test
    public void lookupTaxonPathByLSID() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        eolService.addTaxonInfo(1045608L, properties);
        assertThat(properties.get(PATH), is("Animalia" + CharsetConstant.SEPARATOR
                + "Arthropoda" + CharsetConstant.SEPARATOR
                + "Insecta" + CharsetConstant.SEPARATOR
                + "Hymenoptera" + CharsetConstant.SEPARATOR
                + "Apoidea" + CharsetConstant.SEPARATOR
                + "Apidae" + CharsetConstant.SEPARATOR
                + "Apis" + CharsetConstant.SEPARATOR
                + "Apis mellifera"));
    }

    @Test
    public void lookupTaxonPathByLSIDForPageWithoutClassification() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        eolService.addTaxonInfo(13644436L, properties);
        assertThat(properties.get(PATH), is(nullValue()));
    }

    @Test
    public void lookupRedirectedId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(EXTERNAL_ID, "EOL:10890298");
        }};
        properties.put(NAME, "");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("Anaphes brachygaster"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:1073676"));
    }

    @Test
    public void lookupTaxonPathByScientificNameAlreadySet() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(COMMON_NAMES, "bla bla");
            put(PATH, "bla bla2");
            put(EXTERNAL_ID, "bla bla3");
        }};
        properties.put(NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(COMMON_NAMES), is("bla bla"));
        assertThat(properties.get(PATH), is("bla bla2"));
        assertThat(properties.get(EXTERNAL_ID), is("bla bla3"));

    }

    @Test
    public void lookupCommonNamesOnly() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(PATH, "bla bla2");
            put(EXTERNAL_ID, "bla bla3");
        }};
        properties.put(NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(COMMON_NAMES), containsString("Human"));
        assertThat(properties.get(PATH), containsString("Animalia"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:327955"));
    }

    @Test
    public void lookupToads() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(PATH, "bla bla2");
            put(EXTERNAL_ID, "bla bla3");
        }};
        properties.put(NAME, "Todarodes pacificus");
        properties = eolService.enrich(properties);
        assertThat(properties.get(EXTERNAL_ID), is("EOL:590939"));
        assertThat(properties.get(COMMON_NAMES), containsString("Flying squid"));
        assertThat(properties.get(PATH), containsString("Animalia"));
    }

    @Test
    public void lookupAriopsisFelis() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
        }};
        properties.put(NAME, "Ariopsis felis");
        properties = eolService.enrich(properties);
        assertThat(properties.get(EXTERNAL_ID), is("EOL:223038"));
        assertThat(properties.get(PATH), containsString("Ariopsis felis"));
        assertThat(properties.get(COMMON_NAMES), containsString("@ru"));
    }

    @Test
    @Ignore("re-enabled after EOL fixes https://github.com/jhpoelen/eol-globi-data/issues/175")
    public void lookupBrownCrust() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
        }};
        properties.put(NAME, "Brown");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is(not("Strix leptogrammica")));
        assertThat(properties.get(NAME), is("Brown"));
        assertThat(properties.size(), is(1));
    }

    @Test
    public void lookupBluePlastic() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
        }};
        properties.put(NAME, "blue plastic");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("blue plastic"));
        assertThat(properties.size(), is(1));
    }

    @Test
    @Ignore("re-enabled after EOL fixes https://github.com/jhpoelen/eol-globi-data/issues/175")
    public void lookupBlue() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
        }};
        properties.put(NAME, "blue");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("blue"));
        assertThat(properties.size(), is(1));
    }

    @Test
    @Ignore("re-enabled after EOL fixes https://github.com/jhpoelen/eol-globi-data/issues/175")
    public void lookupWhite() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
        }};
        properties.put(NAME, "white");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("white"));
        assertThat(properties.size(), is(1));
    }

    @Test
    public void lookupTaxonPathByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        hasHomoSapiensPath(properties);
    }

    protected void hasHomoSapiensPath(Map<String, String> properties) {
        assertThat(properties.get(PATH), containsString("Hominidae"));
    }

    @Test
    public void lookupExternalIdByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(EXTERNAL_ID), is("EOL:327955"));
        hasHomoSapiensPath(properties);
    }

    @Test
    public void lookupExternalIdAndPathByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(EXTERNAL_ID), is("EOL:327955"));
        hasHomoSapiensPath(properties);
    }

    @Test
    public void lookupPathNCBIExternalId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        properties.put(EXTERNAL_ID, "NCBI:9606");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("Homo sapiens"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:327955"));
        hasHomoSapiensPath(properties);
    }

    @Test
    public void lookupPathNCBIExternalId2() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Homo sapiens");
        properties.put(EXTERNAL_ID, "NCBI:54642");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PATH_NAMES), is("Homo sapiens"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:327955"));
        hasHomoSapiensPath(properties);
    }

    @Test
    public void lookupPathITISExternalId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "sea otter");
        properties.put(EXTERNAL_ID, "ITIS:180547");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("Enhydra lutris"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:328583"));
        assertThat(properties.get(PATH), is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }

    @Test
    public void lookupPathITISExternalIdInvalidName() throws PropertyEnricherException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/110
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(EXTERNAL_ID, "ITIS:167353");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is(nullValue()));
        assertThat(properties.get(EXTERNAL_ID), is("ITIS:167353"));
        assertThat(properties.get(PATH), is(nullValue()));
    }

    @Test
    public void lookupPathNCBIExternalIdInfluenza() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "something");
        properties.put(EXTERNAL_ID, "NCBI:198585");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("Influenza A virus (A/1352/02(H1N2))"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:11637515"));
        assertThat(properties.get(PATH_NAMES), is("superkingdom |  |  | family | genus | species |  | "));
        assertThat(properties.get(PATH), is("Viruses | Ssrna viruses | Ssrna negative-strand viruses | Orthomyxoviridae | Influenzavirus A | Influenza A virus | H1n2 subtype | Influenza A virus (A/1352/02(H1N2))"));
    }

    @Test
    public void lookupThreePartName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Buteo buteo pojana");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("Buteo buteo pojana"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:4378686"));
        assertThat(properties.get(PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species | infraspecies"));
        assertThat(properties.get(PATH), is("Animalia | Chordata | Aves | Accipitriformes | Accipitridae | Buteo | Buteo buteo | Buteo buteo pojana"));
    }

    @Test
    public void lookupUnresolved() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Platostoma africanum");
        properties = eolService.enrich(properties);
        assertThat(properties.get(NAME), is("Platostoma africanum"));
        assertThat(properties.get(EXTERNAL_ID), is("EOL:5382900"));
        assertThat(properties.get(PATH_NAMES), is(notNullValue()));
        assertThat(properties.get(PATH), is(notNullValue()));
    }

    @Test
    public void lookupExternalIdAndPathByNonScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Other suspension feeders");
        properties = eolService.enrich(properties);
        assertThat(properties.get(EXTERNAL_ID), is(nullValue()));
        assertThat(properties.get(PATH), is(nullValue()));

    }

    @Test
    public void lookupCommonNamesByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, "Rattus rattus");
        properties = eolService.enrich(properties);
        assertThat(properties.get(EXTERNAL_ID), is("EOL:328447"));
        assertThat(properties.get(PATH), containsString("Animalia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PATH), containsString("Chordata" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PATH), containsString("Mammalia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PATH), containsString("Rodentia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PATH), containsString("Rattus" + CharsetConstant.SEPARATOR + "Rattus rattus"));
        String commonNames = properties.get(COMMON_NAMES);
        String expected = "Huisrot @af | Hausratte @de | black rat @en";
        String[] names = expected.split(CharsetConstant.SEPARATOR);
        for (String name : names) {
            assertThat(commonNames, containsString(name));
        }


    }

    @Test
    public void lookupIdsWithDelimiters() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<>();
        properties.put(EXTERNAL_ID, "EOL:392765");
        properties = eolService.enrich(properties);
        String commonNames = properties.get(COMMON_NAMES);
        assertThat(commonNames, is("roble amarillo @en | \"makulis\" @es | เหลืองอินเดีย @th | "));
    }

    @Test
    public void lookupIdsWithDelimiters2() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<>();
        properties.put(EXTERNAL_ID, "EOL:224784");
        properties = eolService.enrich(properties);
        String commonNames = properties.get(COMMON_NAMES);
        String expectedCommonNames = "Kolvin-soldaat @af | Deek @ar | 鐵甲 @cnm | Eichhörnchenfisch @de | Sammara squirrelfish @en | Candil samara @es | Corocoro @fj | Marignan tacheté @fr | \"Ala'ihi @hw | Ukeguchi-ittoudai @ja | 무늬얼게돔 @ko | Jerra @mh | Kolithaduva @ml | Kinolu @ms | Esquilo samara @pt | Malau-tui @sm | Baga-baga @tl | Araoe @ty | Cá Son dá dài @vi | 条纹长颏鳂 @zh | 莎姆新東洋金鱗魚 @zh-Hant | ";
        assertThat(commonNames, is(expectedCommonNames));
    }

}
