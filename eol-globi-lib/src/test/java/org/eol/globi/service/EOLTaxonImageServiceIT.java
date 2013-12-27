package org.eol.globi.service;

import org.junit.Test;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class EOLTaxonImageServiceIT {

    private EOLTaxonImageService serviceTaxon = new EOLTaxonImageService();

    @Test
    public void imageLookupITIS() throws URISyntaxException, IOException {
        assertITISImage(serviceTaxon.lookupImageURLs(TaxonomyProvider.ITIS, "165653"));
        assertITISImage(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_ITIS + "165653"));
    }

    private void assertITISImage(TaxonImage taxonImage) {
        assertThat(taxonImage.getThumbnailURL(), containsString(".jpg"));
        assertThat(taxonImage.getImageURL(), containsString(".jpg"));
        assertThat(taxonImage.getEOLPageId(), is("207614"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/207614"));
        assertThat(taxonImage.getScientificName(), is("Fundulus jenkinsi"));
        assertThat(taxonImage.getCommonName(), is("Topminnows"));
    }

    @Test
    public void imageLookupNCBI() throws URISyntaxException, IOException {
        TaxonImage taxonImage = serviceTaxon.lookupImageURLs(TaxonomyProvider.NCBI, "28806");
        assertThat(taxonImage.getThumbnailURL(), endsWith(".jpg"));
        assertThat(taxonImage.getImageURL(), endsWith(".jpg"));
        assertThat(taxonImage.getEOLPageId(), is("205157"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/205157"));
        assertThat(taxonImage.getScientificName(), is("Centropomus undecimalis"));
        assertThat(taxonImage.getCommonName(), is("Common Snook"));
    }

    @Test
    public void imageLookupWoRMS() throws URISyntaxException, IOException {
        assertWoRMSImage(serviceTaxon.lookupImageURLs(TaxonomyProvider.WORMS, "276287"));
        assertWoRMSImage(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_WORMS + "276287"));
    }

    @Test
    public void imageLookupWoRMSNoEOLPageId() throws IOException {
        TaxonImage taxonImage = serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_WORMS + "585857");
        assertThat(taxonImage.getInfoURL(), notNullValue());
    }

    private void assertWoRMSImage(TaxonImage taxonImage) {
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2009/11/17/11/81513_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2009/11/17/11/81513_orig.jpg"));
        assertThat(taxonImage.getEOLPageId(), is("210779"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/210779"));
        assertThat(taxonImage.getScientificName(), is("Prionotus paralatus"));
        assertThat(taxonImage.getCommonName(), is("Mexican Searobin"));
    }

    @Test
    public void imageLookupEOL() throws URISyntaxException, IOException {
        assertEOLImage(serviceTaxon.lookupImageURLs(TaxonomyProvider.EOL, "1045608"));
        assertEOLImage(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_EOL + "1045608"));
    }

    private void assertEOLImage(TaxonImage taxonImage) {
        assertThat(taxonImage.getThumbnailURL(), containsString(".jpg"));
        assertThat(taxonImage.getImageURL(), containsString(".jpg"));
        assertThat(taxonImage.getEOLPageId(), is("1045608"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/1045608"));
        assertThat(taxonImage.getScientificName(), is("Apis mellifera"));
        assertThat(taxonImage.getCommonName(), is("European Honey Bee"));
    }

    @Test
    public void imageLookupEOL2() throws URISyntaxException, IOException {
        assertEOLImage2(serviceTaxon.lookupImageURLs(TaxonomyProvider.EOL, "2215"));
        assertEOLImage2(serviceTaxon.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_EOL + "2215"));
    }

    private void assertEOLImage2(TaxonImage taxonImage) {
        assertThat(taxonImage.getEOLPageId(), is("2215"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/2215"));
        assertThat(taxonImage.getCommonName(), is("Mussels and Clams"));
        assertThat(taxonImage.getScientificName(), is("Bivalvia"));
    }
}
