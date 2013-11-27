package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class EOLServiceIT {

    public static final String HOMO_SAPIENS_PATH = "Animalia" + CharsetConstant.SEPARATOR +
            "Chordata" + CharsetConstant.SEPARATOR +
            "Vertebrata" + CharsetConstant.SEPARATOR +
            "Mammalia" + CharsetConstant.SEPARATOR +
            "Theria" + CharsetConstant.SEPARATOR +
            "Eutheria" + CharsetConstant.SEPARATOR +
            "Primates" + CharsetConstant.SEPARATOR +
            "Hominidae" + CharsetConstant.SEPARATOR +
            "Homo" + CharsetConstant.SEPARATOR +
            "Homo sapiens";

    @Test
    public void lookupByName() throws TaxonPropertyLookupServiceException {
        assertThat(lookupPageIdByScientificName("Catfish"), is("EOL:204346"));
        assertThat(lookupPageIdByScientificName("Hygrocybe pratensis var. pallida"), is("EOL:6676627"));

        assertThat(lookupPageIdByScientificName("Homo sapiens"), is("EOL:327955"));
        assertThat(lookupPageIdByScientificName("Puccinia caricina var. ribesii-pendulae"), is("EOL:6776658"));
        assertThat(lookupPageIdByScientificName("Hesperocharis paranensis"), is("EOL:176594"));

        assertThat(lookupPageIdByScientificName("Dead roots"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Prunella (Bot)"), is("EOL:70879"));
        assertThat(lookupPageIdByScientificName("Prunella (Bird)"), is("EOL:77930"));
        assertThat(lookupPageIdByScientificName("Pseudobaeospora dichroa"), is("EOL:1001400"));

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
     * @throws TaxonPropertyLookupServiceException
     *
     */

    @Test
    public void multiplySymbolNotSupportedByEOL() throws TaxonPropertyLookupServiceException {
        assertThat(lookupPageIdByScientificName("Salix cinerea \u00D7 phylicifolia"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Salix cinerea × phylicifolia"), is(nullValue()));

    }

    @Test
    public void lookupByNameYieldsMoreThanOneMatches() throws TaxonPropertyLookupServiceException {
        // this species has two matches  http://eol.org/27383107 and http://eol.org/209714, first is picked
        assertThat(lookupPageIdByScientificName("Copadichromis insularis"), is("EOL:209714"));

        // below matches both http://eol.org/4443282 and http://eol.org/310363, but first is picked
        assertThat(lookupPageIdByScientificName("Spilogale putorius gracilis"), is("EOL:310363"));

        // etc
        assertThat(lookupPageIdByScientificName("Crocethia alba"), is("EOL:1049518"));
        assertThat(lookupPageIdByScientificName("Ecdyonuridae"), is("EOL:2762776"));
        assertThat(lookupPageIdByScientificName("Catasticta hegemon"), is("EOL:173526"));
        assertThat(lookupPageIdByScientificName("Theridion ovatum"), is("EOL:1187291"));
        assertThat(lookupPageIdByScientificName("Cambarus propinquus"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Vellidae"), is("EOL:644"));
        assertThat(lookupPageIdByScientificName("Mylothris rueppellii"), is("EOL:180170"));

    }

    @Test
    public void enrichUsingPreExistingEOLPageId() throws TaxonPropertyLookupServiceException {
        EOLService eolService = new EOLService();
        //warm up
        HashMap<String, String> properties = new HashMap<String, String>();
        eolService.lookupPropertiesByName("Homo sapiens", properties);

        properties = new HashMap<String, String>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        eolService.lookupPropertiesByName("Homo sapiens", properties);
        stopWatch.stop();
        long durationFullLookup = stopWatch.getTime();
        assertThat(properties.get(Taxon.COMMON_NAMES), is(notNullValue()));
        assertThat(properties.get(Taxon.PATH), is(notNullValue()));
        assertThat(properties.get(Taxon.EXTERNAL_ID), is(notNullValue()));
        final String externalId = properties.get(Taxon.EXTERNAL_ID);

        HashMap<String, String> enrichedProperties = new HashMap<String, String>() {{
            put(Taxon.EXTERNAL_ID, externalId);
        }};

        stopWatch.reset();
        stopWatch.start();
        eolService.lookupPropertiesByName("Homo sapiens", enrichedProperties);
        stopWatch.stop();

        long durationNonPageIdLookup = stopWatch.getTime();

        assertThat("expected full lookup [" + durationFullLookup + "] ms, to be slower than partial lookup [" + durationNonPageIdLookup + "] ms",
                durationNonPageIdLookup < durationFullLookup, is(true));

        assertThat(properties.get(Taxon.COMMON_NAMES), is(notNullValue()));
        assertThat(properties.get(Taxon.PATH), is(notNullValue()));
        assertThat(properties.get(Taxon.EXTERNAL_ID), is(notNullValue()));
    }

    @Test
    public void lookupByNameYieldsNoMatches() throws TaxonPropertyLookupServiceException {
        assertThat(lookupPageIdByScientificName("Clio acicula"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Aegires oritzi"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Fish hook"), is(nullValue()));
    }

    private String lookupPageIdByScientificName(String taxonName) throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().lookupPropertiesByName(taxonName, properties);
        return properties.get(Taxon.EXTERNAL_ID);
    }

    @Test
    public void parsePageIdEnsureLowestIsSelected() {
        String response = pageFeedResponsePrefix();
        response += biggerPageId();
        response += smallerPageId();
        response += pageFeedResponseSuffix();

        Long actual = new EOLService().findSmallestPageId(response);
        assertThat(actual, is(310363L));

        response = pageFeedResponsePrefix() + smallerPageId() + biggerPageId() + pageFeedResponseSuffix();
        actual = new EOLService().findSmallestPageId(response);
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
    public void lookupTaxonPathByLSID() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().addPathAndCommonNames(1045608L, properties);
        assertThat(properties.get(Taxon.PATH), Is.is("Animalia" + CharsetConstant.SEPARATOR
                + "Arthropoda" + CharsetConstant.SEPARATOR
                + "Insecta" + CharsetConstant.SEPARATOR
                + "Hymenoptera" + CharsetConstant.SEPARATOR
                + "Apoidea" + CharsetConstant.SEPARATOR
                + "Apidae" + CharsetConstant.SEPARATOR
                + "Apis" + CharsetConstant.SEPARATOR
                + "Apis mellifera"));
    }

    @Test
    public void lookupTaxonPathByLSIDForPageWithoutClassification() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().addPathAndCommonNames(13644436L, properties);
        assertThat(properties.get(Taxon.PATH), Is.is(nullValue()));
    }

    @Test
    public void lookupTaxonPathByScientificNameAlreadySet() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put(Taxon.COMMON_NAMES, "bla bla");
            put(Taxon.PATH, "bla bla2");
            put(Taxon.EXTERNAL_ID, "bla bla3");
        }};
        new EOLService().lookupPropertiesByName("Homo sapiens", properties);
        assertThat(properties.get(Taxon.COMMON_NAMES), Is.is("bla bla"));
        assertThat(properties.get(Taxon.PATH), Is.is("bla bla2"));
        assertThat(properties.get(Taxon.EXTERNAL_ID), Is.is("bla bla3"));

    }

    @Test
    public void lookupCommonNamesOnly() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put(Taxon.PATH, "bla bla2");
            put(Taxon.EXTERNAL_ID, "bla bla3");
        }};
        new EOLService().lookupPropertiesByName("Homo sapiens", properties);
        assertThat(properties.get(Taxon.COMMON_NAMES), containsString("Human"));
        assertThat(properties.get(Taxon.PATH), containsString("Animalia"));
        assertThat(properties.get(Taxon.EXTERNAL_ID), Is.is("EOL:327955"));

    }

    @Test
    public void lookupTaxonPathByScientificName() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().lookupPropertiesByName("Homo sapiens", properties);
        assertThat(properties.get(Taxon.PATH), Is.is(HOMO_SAPIENS_PATH));

    }

    @Test
    public void lookupExternalIdByScientificName() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().lookupPropertiesByName("Homo sapiens", properties);
        assertThat(properties.get(Taxon.EXTERNAL_ID), Is.is("EOL:327955"));
        assertThat(properties.get(Taxon.PATH), Is.is(HOMO_SAPIENS_PATH));
    }

    @Test
    public void lookupExternalIdAndPathByScientificName() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().lookupPropertiesByName("Homo sapiens", properties);
        assertThat(properties.get(Taxon.EXTERNAL_ID), Is.is("EOL:327955"));
        assertThat(properties.get(Taxon.PATH), Is.is(HOMO_SAPIENS_PATH));

    }

    @Test
    public void lookupExternalIdAndPathByNonScientificName() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().lookupPropertiesByName("Other suspension feeders", properties);
        assertThat(properties.get(Taxon.EXTERNAL_ID), Is.is(nullValue()));
        assertThat(properties.get(Taxon.PATH), Is.is(nullValue()));

    }

    @Test
    public void lookupCommonNamesByScientificName() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new EOLService().lookupPropertiesByName("Rattus rattus", properties);
        assertThat(properties.get(Taxon.EXTERNAL_ID), Is.is("EOL:328447"));
        assertThat(properties.get(Taxon.PATH), containsString("Animalia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(Taxon.PATH), containsString("Chordata" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(Taxon.PATH), containsString("Mammalia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(Taxon.PATH), containsString("Rodentia" + CharsetConstant.SEPARATOR));
        assertThat(properties.get(Taxon.PATH), containsString("Rattus" + CharsetConstant.SEPARATOR + "Rattus rattus"));
        String commonNames = properties.get(Taxon.COMMON_NAMES);
        String expected = "Huisrot @af" + CharsetConstant.SEPARATOR + "جرذ المنزل @ar" + CharsetConstant.SEPARATOR + "Hausratte @de" + CharsetConstant.SEPARATOR + "black rat @en" + CharsetConstant.SEPARATOR + "Rata negra @es" + CharsetConstant.SEPARATOR + "rat noir @fr" + CharsetConstant.SEPARATOR + "Чёрная крыса @ru" + CharsetConstant.SEPARATOR + "家鼠 @zh" + CharsetConstant.SEPARATOR + "屋顶鼠 @zh-Hans" + CharsetConstant.SEPARATOR + "";
        String[] names = expected.split(CharsetConstant.SEPARATOR);
        for (String name : names) {
            assertThat(commonNames, containsString(name));
        }


    }

}
