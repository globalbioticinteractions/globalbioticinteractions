package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLServiceIT {

    @Test
    public void lookupByName() throws TaxonPropertyLookupServiceException {
        assertThat(lookupPageIdByScientificName("Hygrocybe pratensis var. pallida"), is("EOL:6676627"));

        assertThat(lookupPageIdByScientificName("Homo sapiens"), is("EOL:327955"));
        assertThat(lookupPageIdByScientificName("Puccinia caricina var. ribesii-pendulae"), is("EOL:6776658"));
        assertThat(lookupPageIdByScientificName("Hesperocharis paranensis"), is("EOL:176594"));
        // TODO need to find a way to include only pages that have at least one external taxonomy
        assertThat(lookupPageIdByScientificName("Dead roots"), is("EOL:19665069"));
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
        assertThat(lookupPageIdByScientificName("Cambarus propinquus"), is("EOL:4260550"));
        assertThat(lookupPageIdByScientificName("Vellidae"), is("EOL:644"));
        assertThat(lookupPageIdByScientificName("Mylothris rueppellii"), is("EOL:180170"));

    }

    @Test
    public void lookupByNameYieldsNoMatches() throws TaxonPropertyLookupServiceException {
        assertThat(lookupPageIdByScientificName("Clio acicula"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Aegires oritzi"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Fish hook"), is(nullValue()));
    }

    private String lookupPageIdByScientificName(String taxonName) throws TaxonPropertyLookupServiceException {
        return new EOLService().lookupIdByName(taxonName);
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
        String rank = new EOLService().lookupTaxonPathByLSID("EOL:1045608");
        assertThat(rank, Is.is("Animalia Arthropoda Insecta Hymenoptera Apoidea Apidae Apis Apis mellifera"));
    }

    @Test
    public void lookupTaxonPathByLSIDForPageWithoutClassification() throws TaxonPropertyLookupServiceException {
        String rank = new EOLService().lookupTaxonPathByLSID("EOL:13644436");
        assertThat(rank, Is.is(nullValue()));
    }

    @Test
    public void lookupTaxonPathByScientificName() throws TaxonPropertyLookupServiceException {
        String taxonRank = new EOLService().lookupPropertyValueByTaxonName("Homo sapiens", Taxon.PATH);
        assertThat(taxonRank, Is.is("Animalia Chordata Vertebrata Mammalia Theria Eutheria Primates Hominidae Homo Homo sapiens"
        ));
    }

}
