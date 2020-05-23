package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.SearchContext;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class WikiDataImageSearchTest {

    @Test
    public void lookupLion() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q140");
        assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void lookupLionByNCBI() throws IOException {
        // NCBI https://www.wikidata.org/wiki/Property:P685
        // iNaturalist https://www.wikidata.org/wiki/Property:P3151
        // GBIF https://www.wikidata.org/wiki/Property:P846
        // EOL https://www.wikidata.org/wiki/Property:P830
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NCBI:9689");
        assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void createITISLionQuery() {
        String sparqlQuery = new WikiDataImageSearch().createSparqlQuery("ITIS:183803", "en");
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
    public void createWikiDataLionQuery() {
        String sparqlQuery = new WikiDataImageSearch().createSparqlQuery("WD:Q140", "en");
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
        // ITIS https://www.wikidata.org/wiki/Property:P815
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("ITIS:183803");
        assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void lookupRedVole() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q608821");
        assertNotNull(taxonImage);
        assertThat(taxonImage.getCommonName(), is("Northern Red-backed Vole, Red Vole @en"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q608821"));
    }

    @Test
    public void northernBat() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NBN:NHMSYS0000528007");
        assertNotNull(taxonImage);
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
        assertNotNull(taxonImage);
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
        assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(taxonImage.getCommonName(), is(nullValue()));
    }

    @Test
    public void lookupUnsupported() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("foo:bar");
        assertNull(taxonImage);
    }

}