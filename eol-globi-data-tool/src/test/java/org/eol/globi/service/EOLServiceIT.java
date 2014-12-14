package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class EOLServiceIT {

    public static final String HOMO_SAPIENS_PATH = "Animalia" + CharsetConstant.SEPARATOR +
            "Chordata" + CharsetConstant.SEPARATOR +
            "Mammalia" + CharsetConstant.SEPARATOR +
            "Primates" + CharsetConstant.SEPARATOR +
            "Hominidae" + CharsetConstant.SEPARATOR +
            "Homo" + CharsetConstant.SEPARATOR +
            "Homo sapiens";
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
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        EOLService eolService = this.eolService;
        eolService.setFilter(new PropertyEnrichmentFilter() {
            @Override
            public boolean shouldReject(Map<String, String> properties) {
                return true;
            }
        });
        Map<String, String> props = eolService.enrich(properties);
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));

        eolService.setFilter(new PropertyEnrichmentFilter() {
            @Override
            public boolean shouldReject(Map<String, String> properties) {
                return false;
            }
        });
        props = eolService.enrich(properties);
        assertThat(props.get(PropertyAndValueDictionary.EXTERNAL_ID), is(notNullValue()));
    }

    @Test
    public void nonTaxonPage() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:29725463");
        Map<String, String> enrich = new EOLService().enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
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
    // see https://github.com/jhpoelen/eol-globi-data/issues/60
    public void gallTissue() throws PropertyEnricherException {
        assertThat(lookupPageIdByName("gall tissue"), is(nullValue()));
        assertThat(lookupPageIdByName("gall"), is("EOL:210208"));
    }

    @Test
    public void lookupBySquatLobster() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Squat lobster");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:315099"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Munidopsis albatrossae"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
    }

    @Test
    public void lookupByAcheloussprinicarpusUTF8() throws PropertyEnricherException {
        String[] names = new String[]{"Acheloüs spinicarpus", "Achelous spinicarpus"};
        for (String name : names) {
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(PropertyAndValueDictionary.NAME, name);
            Map<String, String> enrich = eolService.enrich(properties);
            assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:343000"));
            assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Acheloüs spinicarpus"));
            assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        }
    }

    @Test
    public void lookupPickleweed() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Pickleweed");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:61812"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Salicornia"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Genus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Plantae | Tracheophyta | Magnoliopsida | Caryophyllales | Chenopodiaceae | Salicornia"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus"));

    }

    @Test
    public void lookupCalyptridium() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Calyptridium");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:2500577"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Calyptridium"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Genus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Cellular organisms | Eukaryota | Viridiplantae | Streptophyta | Streptophytina | Embryophyta | Tracheophyta | Euphyllophyta | Spermatophyta | Magnoliophyta | Mesangiospermae | Eudicotyledons | Gunneridae | Pentapetalae | Caryophyllales | Cactineae | Montiaceae | Calyptridium"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is(" | superkingdom | kingdom | phylum |  |  |  |  |  |  |  |  |  |  | order | suborder | family | genus"));
    }

    @Test
    public void lookupPyrguscirsii() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Pyrgus cirsii");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:186021"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Pyrgus cirsii"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Arthropoda | Insecta | Lepidoptera | Hesperiidae | Pyrgus | Pyrgus cirsii"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | "));
    }

    @Test
    public void sphyrnaMokarran() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Sphyrna mokarran");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Elasmobranchii | Carcharhiniformes | Sphyrnidae | Sphyrna | Sphyrna mokarran"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("Species"));
    }

    @Test
    public void suspensionFeeders() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Other suspension feeders");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is(nullValue()));
    }

    @Test
    public void lookupPickleweedAlreadyEnriched() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:61812");
        properties.put(PropertyAndValueDictionary.NAME, "a name");
        properties.put(PropertyAndValueDictionary.RANK, "a rank");
        properties.put(PropertyAndValueDictionary.PATH, "a path");
        properties.put(PropertyAndValueDictionary.COMMON_NAMES, "a common name");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:61812"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("a name"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("a rank"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("a path"));
    }

    @Test
    public void lookupHake() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Hake");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:205098"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Merluccius bilinearis"));
    }

    @Test
    public void lookupSupportedByNonEOLId() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        String nonEOLId = TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY + "1235";
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, nonEOLId);
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nonEOLId));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is(nullValue()));
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

    @Test
    public void enrichUsingPreExistingEOLPageId() throws PropertyEnricherException {
        //warm up
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        eolService.enrich(properties);

        properties = new HashMap<String, String>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = eolService.enrich(properties);
        stopWatch.stop();
        long durationFullLookup = stopWatch.getTime();
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(notNullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is(notNullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(notNullValue()));
        final String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);

        HashMap<String, String> enrichedProperties = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        }};

        stopWatch.reset();
        stopWatch.start();
        enrichedProperties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        enrich = eolService.enrich(enrichedProperties);
        stopWatch.stop();

        long durationNonPageIdLookup = stopWatch.getTime();

        assertThat("expected full lookup [" + durationFullLookup + "] ms, to be slower than partial lookup [" + durationNonPageIdLookup + "] ms",
                durationNonPageIdLookup < durationFullLookup, is(true));

        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is(notNullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is(notNullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(notNullValue()));
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
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "foo:bar");
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), not(is("foo:bar")));

    }

    // see https://github.com/jhpoelen/eol-globi-data/issues/77
    @Test
    public void lookupEOLExternalIdNoClassification() throws PropertyEnricherException {
        String[] externalIds = {"EOL:3821293", "EOL:3238626", "EOL:17264771", "EOL:3825406"};
        for (String externalId : externalIds) {
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
            properties.put(PropertyAndValueDictionary.NAME, null);
            properties = eolService.enrich(properties);
            assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
        }
    }

    @Test
    public void lookupEOLExternalId2() throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "foo:bar");
        properties.put(PropertyAndValueDictionary.NAME, "Prunella vulgaris");
        Map<String, String> enrich = eolService.enrich(properties);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:579652"));

    }

    private String lookupPageIdByName(String taxonName) throws PropertyEnricherException {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, taxonName);
        Map<String, String> enrich = eolService.enrich(properties);
        return enrich.get(PropertyAndValueDictionary.EXTERNAL_ID);
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
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is("Animalia" + CharsetConstant.SEPARATOR
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
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is(nullValue()));
    }

    @Test
    public void lookupRedirectedId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:10890298");
        }};
        properties.put(PropertyAndValueDictionary.NAME, "");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Anaphes brachygaster"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:1073676"));
    }

    @Test
    public void lookupTaxonPathByScientificNameAlreadySet() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.COMMON_NAMES, "bla bla");
            put(PropertyAndValueDictionary.PATH, "bla bla2");
            put(PropertyAndValueDictionary.EXTERNAL_ID, "bla bla3");
        }};
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.COMMON_NAMES), Is.is("bla bla"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is("bla bla2"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("bla bla3"));

    }

    @Test
    public void lookupCommonNamesOnly() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.PATH, "bla bla2");
            put(PropertyAndValueDictionary.EXTERNAL_ID, "bla bla3");
        }};
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Human"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Animalia"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:327955"));
    }

    @Test
    public void lookupToads() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.PATH, "bla bla2");
            put(PropertyAndValueDictionary.EXTERNAL_ID, "bla bla3");
        }};
        properties.put(PropertyAndValueDictionary.NAME, "Todarodes pacificus");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:590939"));
        assertThat(properties.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("flying squid"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Animalia"));
    }

    @Test
    public void lookupAriopsisFelis() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {{
        }};
        properties.put(PropertyAndValueDictionary.NAME, "Ariopsis felis");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:223038"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Ariopsis felis"));
    }

    @Test
    public void lookupTaxonPathByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is(HOMO_SAPIENS_PATH));
    }

    @Test
    public void lookupExternalIdByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:327955"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is(HOMO_SAPIENS_PATH));
    }

    @Test
    public void lookupExternalIdAndPathByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:327955"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is(HOMO_SAPIENS_PATH));

    }

    @Test
    public void lookupPathNCBIExternalId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:9606");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), Is.is("Homo sapiens"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:327955"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is(HOMO_SAPIENS_PATH));

    }

    @Test
    public void lookupPathITISExternalId() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "sea otter");
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:180547");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), Is.is("Enhydra lutris"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:328583"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }

    @Test
    public void lookupExternalIdAndPathByNonScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Other suspension feeders");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is(nullValue()));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), Is.is(nullValue()));

    }

    @Test
    public void lookupCommonNamesByScientificName() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.NAME, "Rattus rattus");
        properties = eolService.enrich(properties);
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), Is.is("EOL:328447"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Animalia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Chordata" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Mammalia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Rodentia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Rattus" + CharsetConstant.SEPARATOR + "Rattus rattus"));
        String commonNames = properties.get(PropertyAndValueDictionary.COMMON_NAMES);
        String expected = "Huisrot @af" + CharsetConstant.SEPARATOR + "جرذ المنزل @ar" + CharsetConstant.SEPARATOR + "Hausratte @de" + CharsetConstant.SEPARATOR + "black rat @en" + CharsetConstant.SEPARATOR + "Rata negra @es" + CharsetConstant.SEPARATOR + "rat noir @fr" + CharsetConstant.SEPARATOR + "Чёрная крыса @ru" + CharsetConstant.SEPARATOR + "家鼠 @zh" + CharsetConstant.SEPARATOR + "屋顶鼠 @zh-Hans" + CharsetConstant.SEPARATOR + "";
        String[] names = expected.split(CharsetConstant.SEPARATOR);
        for (String name : names) {
            assertThat(commonNames, containsString(name));
        }


    }

}
