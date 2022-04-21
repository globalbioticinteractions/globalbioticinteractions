package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.SearchContext;
import org.eol.globi.service.WikidataUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WikiDataImageSearchTest {

    @Test
    public void lookupLion() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q140");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void lookupLionByNCBI() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NCBI:9689");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void lookupLionByNCBI2() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NCBI:1000587");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void createITISLionQuery() {
        String sparqlQuery = WikidataUtil.createSparqlQuery("ITIS:183803", "en");
        assertThat(sparqlQuery, is("SELECT ?item ?pic ?name ?wdpage WHERE {\n" +
                "  ?wdpage wdt:P18 ?pic .\n" +
                "  ?wdpage wdt:P815 \"183803\" .\n" +
                "  SERVICE wikibase:label {\n" +
                "   bd:serviceParam wikibase:language \"en\" .\n" +
                "   ?wdpage wdt:P1843 ?name .\n" +
                "  }\n" +
                "} limit 1"));
    }

    @Test
    public void createPlaziRhinolophusDentiQuery() {
        String sparqlQuery = WikidataUtil.createSparqlQuery("PLAZI:885887A2FFC88A21F8B1FA48FB92DD65", "en");
        assertThat(sparqlQuery, is("SELECT ?item ?pic ?name ?wdpage WHERE {\n" +
                "  ?wdpage wdt:P18 ?pic .\n" +
                "  ?wdpage wdt:P1992 \"885887A2FFC88A21F8B1FA48FB92DD65\" .\n" +
                "  SERVICE wikibase:label {\n" +
                "   bd:serviceParam wikibase:language \"en\" .\n" +
                "   ?wdpage wdt:P1843 ?name .\n" +
                "  }\n" +
                "} limit 1"));
    }

    @Test
    public void createWikiDataLionQuery() {
        String sparqlQuery = WikidataUtil.createSparqlQuery("WD:Q140", "en");
        assertThat(sparqlQuery, is("SELECT ?item ?pic ?name WHERE {\n" +
                "  wd:Q140 wdt:P18 ?pic .\n" +
                "  SERVICE wikibase:label {\n" +
                "    bd:serviceParam wikibase:language \"en\" .\n" +
                "    wd:Q140 wdt:P1843 ?name .\n" +
                "  }\n" +
                "} limit 1"));
    }

    @Test
    public void lookupLionByITIS() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("ITIS:183803");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void lookupRedVole() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q608821");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getCommonName(), is("Northern Red-backed Vole, Red Vole @en"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q608821"));
    }

    @Test
    public void northernBat() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NBN:NHMSYS0000528007");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getCommonName(), is("Northern Bat @en"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q300941"));
    }

    @Test
    public void lookupLionJapanese() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q140", new SearchContext() {
            @Override
            public String getPreferredLanguage() {
                return "ja";
            }
        });
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(taxonImage.getCommonName(), is("ライオン (raion) @ja"));
    }

    @Test
    public void lookupLionUnknownLanguage() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q140", new SearchContext() {
            @Override
            public String getPreferredLanguage() {
                return "foo";
            }
        });
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(taxonImage.getCommonName(), is(nullValue()));
    }

    @Test
    public void lookupUnsupported() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("foo:bar");
        Assert.assertNull(taxonImage);
    }

}