package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class WikiDataImageSearchTest {

    @Test
    public void lookupLion() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:Q140");
        assertNotNull(taxonImage);
        assertThat(taxonImage.getThumbnailURL(), startsWith("http://commons.wikimedia.org"));
        assertThat(taxonImage.getInfoURL(), is("https://www.wikidata.org/wiki/Q140"));
    }

    @Test
    public void lookupUnsupported() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("foo:bar");
        assertNull(taxonImage);
    }

}