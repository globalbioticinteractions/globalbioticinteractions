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
        assertThat(lookupPageIdByScientificName("Homo sapiens"), is("EOL:327955"));
        assertThat(lookupPageIdByScientificName("Puccinia caricina var. ribesii-pendulae"), is(nullValue()));
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
        assertThat(lookupPageIdByScientificName("Salix cinerea x phylicifolia"), is("EOL:584272"));
        assertThat(lookupPageIdByScientificName("Salix cinerea \u00D7 phylicifolia"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Salix cinerea × phylicifolia"), is(nullValue()));

    }

    @Test
    public void lookupByNameYieldsMoreThanOneMatches() throws TaxonPropertyLookupServiceException {
        // this species has two matches  http://eol.org/27383107 and http://eol.org/209714, first is picked
        assertThat(lookupPageIdByScientificName("Copadichromis insularis"), is("EOL:209714"));

        // below matches both http://eol.org/4443282 and http://eol.org/310363, but first is picked
        assertThat(lookupPageIdByScientificName("Spilogale putorius gracilis"), is("EOL:4443282"));

        // etc
        assertThat(lookupPageIdByScientificName("Crocethia alba"), is("EOL:4373991"));
        assertThat(lookupPageIdByScientificName("Ecdyonuridae"), is("EOL:19663261"));
        assertThat(lookupPageIdByScientificName("Catasticta hegemon"), is("EOL:173526"));
        assertThat(lookupPageIdByScientificName("Theridion ovatum"), is("EOL:3180388"));
        assertThat(lookupPageIdByScientificName("Cambarus propinquus"), is("EOL:31345356"));
        assertThat(lookupPageIdByScientificName("Zoanthus flosmarinus"), is("EOL:13029466"));
        assertThat(lookupPageIdByScientificName("Vellidae"), is("EOL:644"));
        assertThat(lookupPageIdByScientificName("Mylothris spica"), is("EOL:180160"));
        assertThat(lookupPageIdByScientificName("Mylothris rueppellii"), is("EOL:180170"));

    }

    @Test
    public void lookupByNameYieldsNoMatches() throws TaxonPropertyLookupServiceException {
        assertThat(lookupPageIdByScientificName("Clio acicula"), is(nullValue()));
        assertThat(lookupPageIdByScientificName("Aegires oritzi"), is(nullValue()));
    }

    private String lookupPageIdByScientificName(String taxonName) throws TaxonPropertyLookupServiceException {
        return new EOLService().lookupLSIDByTaxonName(taxonName);
    }

    @Test
    public void lookupTaxonPathByLSID() throws TaxonPropertyLookupServiceException {
        String rank = new EOLService().lookupTaxonPathByLSID("EOL:1045608");
        assertThat(rank, Is.is("Animalia Arthropoda Insecta Hymenoptera Apoidea Apidae Apis"));
    }

    @Test
    public void lookupTaxonPathByLSIDForPageWithoutClassification() throws TaxonPropertyLookupServiceException {
        String rank = new EOLService().lookupTaxonPathByLSID("EOL:13644436");
        assertThat(rank, Is.is(nullValue()));
    }

    @Test
    public void lookupTaxonPathByScientificName() throws TaxonPropertyLookupServiceException {
        String taxonRank = new EOLService().lookupPropertyValueByTaxonName("Homo sapiens", Taxon.PATH);
        assertThat(taxonRank, Is.is("Animalia Chordata Vertebrata Mammalia Theria Eutheria Primates Hominidae Homo"
        ));
    }

}
