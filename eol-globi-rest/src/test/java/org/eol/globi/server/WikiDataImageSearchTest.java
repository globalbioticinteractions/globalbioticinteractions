package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class WikiDataImageSearchTest {

    @Test
    public void lookupLion() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("WD:140");
        assertNotNull(taxonImage);
        assertNotNull(taxonImage.getThumbnailURL());
        assertNotNull(taxonImage.getInfoURL());
    }

    @Test
    public void lookupUnsupported() throws IOException {
        TaxonImage taxonImage = new WikiDataImageSearch().lookupImageForExternalId("foo:bar");
        assertNull(taxonImage);
    }

}