package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.SearchContext;
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
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NCBI:9689");
        assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is("African Lion, Lion @en"));
    }

    @Test
    public void createITISLionQuery() {
        String sparqlQuery = WikiDataImageSearch.createSparqlQuery("ITIS:183803", "en");
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
        String sparqlQuery = WikiDataImageSearch.createSparqlQuery("WD:Q140", "en");
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

    @Test
    public void lookupProviders() throws IOException, URISyntaxException {
        Collection<String> providers = new WikiDataImageSearch().findTaxonIdProviders();
        final Collection<String> properties = WikiDataImageSearch.PROVIDER_TO_WIKIDATA.values();
        assertThat(properties, everyItem(isIn(providers)));
    }

    @Test
    public void lookupWikiDataToProviders() throws IOException, URISyntaxException {
        Collection<String> providers = new WikiDataImageSearch().findTaxonIdProviders();
        final Collection<String> properties = WikiDataImageSearch.WIKIDATA_TO_PROVIDER.keySet();
        assertThat(properties, everyItem(isIn(providers)));
    }

    @Test
    public void lookupTaxonLinks() throws IOException, URISyntaxException {
        Map<TaxonomyProvider, String> relatedTaxonIds =
                new WikiDataImageSearch().findRelatedTaxonIds("NCBI:9606");

        assertThat(relatedTaxonIds, is(new TreeMap<TaxonomyProvider, String>() {{
            put(TaxonomyProvider.ITIS,"180092");
            put(TaxonomyProvider.NBN, "NHMSYS0000376773");
            put(TaxonomyProvider.NCBI, "9606");
            put(TaxonomyProvider.EOL, "327955");
            put(TaxonomyProvider.GBIF, "2436436");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA, "10857762");
            put(TaxonomyProvider.INATURALIST_TAXON, "43584");
            put(TaxonomyProvider.WIKIDATA, "Q15978631");
            put(TaxonomyProvider.MSW, "12100795");
        }}));
    }

    @Test
    public void lookupTaxonLinksByWDEntry() throws IOException, URISyntaxException {
        Map<TaxonomyProvider, String> relatedTaxonIds =
                new WikiDataImageSearch().findRelatedTaxonIds("WD:Q15978631");

        assertThat(relatedTaxonIds, is(new TreeMap<TaxonomyProvider, String>() {{
            put(TaxonomyProvider.ITIS,"180092");
            put(TaxonomyProvider.NBN, "NHMSYS0000376773");
            put(TaxonomyProvider.NCBI, "9606");
            put(TaxonomyProvider.EOL, "327955");
            put(TaxonomyProvider.GBIF, "2436436");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA, "10857762");
            put(TaxonomyProvider.INATURALIST_TAXON, "43584");
            put(TaxonomyProvider.WIKIDATA, "Q15978631");
            put(TaxonomyProvider.MSW, "12100795");
        }}));
    }


}