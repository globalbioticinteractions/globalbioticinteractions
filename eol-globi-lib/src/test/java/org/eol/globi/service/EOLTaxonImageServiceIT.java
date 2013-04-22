package org.eol.globi.service;

import org.junit.Test;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLTaxonImageServiceIT {

    private EOLTaxonImageService serviceTaxon = new EOLTaxonImageService();

    @Test
    public void imageLookupITIS() throws URISyntaxException, IOException {
        assertITISImage(serviceTaxon.lookupImageURLs(TaxonomyProvider.ITIS, "165653"));
        assertITISImage(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_ITIS  + "165653"));
    }

    private void assertITISImage(TaxonImage taxonImage) {
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2012/06/14/21/89792_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2012/06/14/21/89792_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("207614"));
    }

    @Test
    public void imageLookupNCBI() throws URISyntaxException, IOException {
        TaxonImage taxonImage = serviceTaxon.lookupImageURLs(TaxonomyProvider.NCBI, "28806");
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2012/06/15/09/03561_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2012/06/15/09/03561_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("205157"));
    }

    @Test
    public void imageLookupWoRMS() throws URISyntaxException, IOException {
        assertWoRMSImage(serviceTaxon.lookupImageURLs(TaxonomyProvider.WORMS, "276287"));
        assertWoRMSImage(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_WORMS + "276287"));
    }

    private void assertWoRMSImage(TaxonImage taxonImage) {
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2009/11/17/11/81513_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2009/11/17/11/81513_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("210779"));
    }

    @Test
    public void imageLookupEOL() throws URISyntaxException, IOException {
        assertEOLImage(serviceTaxon.lookupImageURLs(TaxonomyProvider.EOL, "EOL:276287"));
        assertEOLImage(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_EOL + "276287"));
    }

    private void assertEOLImage(TaxonImage taxonImage) {
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2011/12/13/21/66989_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2011/12/13/21/66989_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("276287"));
    }
}
