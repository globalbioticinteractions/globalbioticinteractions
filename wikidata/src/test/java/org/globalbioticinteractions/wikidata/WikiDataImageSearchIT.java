package org.globalbioticinteractions.wikidata;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.SearchContext;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

public class WikiDataImageSearchIT {

    public static final String LION_COMMON_NAMES = "African Lion, Lion @en";
    public static final String SEA_OTTER_IMAGE = "https://commons.wikimedia.org/wiki/Special:FilePath/Sea%20otter%20nursing.jpg?width=100";

    @Test
    public void lookupLion() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q140");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
        assertThat(taxonImage.getCommonName(), is(LION_COMMON_NAMES));
    }

    @Test
    public void lookupLionByNCBI() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("NCBI:9689");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is(LION_COMMON_NAMES));
    }



    @Test
    public void lookupLionByITIS() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("ITIS:183803");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("https://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q140"));
        assertThat(taxonImage.getCommonName(), is(LION_COMMON_NAMES));
    }

    @Test
    public void lookupRedVole() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q608821");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getCommonName(), is("Northern Red-backed Vole, Red Vole @en"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q608821"));
    }

    @Test
    public void lookupSeaOtter() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q41407");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getCommonName(), is("Sea Otter @en"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q41407"));
        assertThat(taxonImage.getThumbnailURL(), is(SEA_OTTER_IMAGE));
    }

    @Test
    public void lookupSeaOtterEOLIdRetired() throws IOException {
        Assert.assertNull(new WikiDataImageSearch().lookupImageForExternalId("EOL:242598"));
    }

    @Test
    public void lookupSeaOtterEOLIdCurrent() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("EOL:46559130");
        Assert.assertNotNull(taxonImage);
        assertThat(taxonImage.getCommonName(), is("Sea Otter @en"));
        assertThat(taxonImage.getInfoURL(), is("http://www.wikidata.org/entity/Q41407"));
        assertThat(taxonImage.getThumbnailURL(), is(SEA_OTTER_IMAGE));
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
        assertThat(taxonImage.getCommonName(), is("ライオン @ja"));
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