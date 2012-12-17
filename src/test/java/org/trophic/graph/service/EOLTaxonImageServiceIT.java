package org.trophic.graph.service;

import org.junit.Test;
import org.trophic.graph.domain.TaxonImage;
import org.trophic.graph.domain.TaxonomyProvider;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLTaxonImageServiceIT {

    @Test
    public void imageLookupITIS() throws URISyntaxException, IOException {
        EOLTaxonImageService serviceTaxon = new EOLTaxonImageService();
        TaxonImage taxonImage = serviceTaxon.lookupImageURLs(TaxonomyProvider.ITIS, "165653");
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2012/06/14/21/89792_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2012/06/14/21/89792_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("207614"));
    }

    @Test
    public void imageLookupNCBI() throws URISyntaxException, IOException {
        EOLTaxonImageService serviceTaxon = new EOLTaxonImageService();
        TaxonImage taxonImage = serviceTaxon.lookupImageURLs(TaxonomyProvider.NCBI, "28806");
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2012/06/15/09/03561_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2012/06/15/09/03561_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("205157"));
    }

    @Test
    public void imageLookupWoRMS() throws URISyntaxException, IOException {
        EOLTaxonImageService serviceTaxon = new EOLTaxonImageService();
        TaxonImage taxonImage = serviceTaxon.lookupImageURLs(TaxonomyProvider.WORMS, "276287");
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2009/11/17/11/81513_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2009/11/17/11/81513_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("210779"));
    }
}
