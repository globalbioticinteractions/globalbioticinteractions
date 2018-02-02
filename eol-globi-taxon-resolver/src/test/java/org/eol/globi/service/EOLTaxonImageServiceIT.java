package org.eol.globi.service;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class EOLTaxonImageServiceIT {

    private ImageSearch imageService = new EOLTaxonImageService();

    @Test
    public void imageLookupITISTaxon() throws URISyntaxException, IOException {

        assertITISImage(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_ITIS + "165653"));
    }

    @Test
    public void imageLookupITIS() throws URISyntaxException, IOException {
        assertITISImage(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_ITIS + "165653"));
    }

    private void assertITISImage(TaxonImage taxonImage) {
        assertThat(taxonImage, is(notNullValue()));
        assertThat(taxonImage.getThumbnailURL(), containsString(".jpg"));
        assertThat(taxonImage.getImageURL(), containsString(".jpg"));
        assertThat(taxonImage.getPageId(), is("207614"));
        assertThat(taxonImage.getInfoURL(), is("http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=165653"));
        assertThat(taxonImage.getScientificName(), is("Fundulus jenkinsi"));
        assertThat(taxonImage.getCommonName(), is("Topminnows"));
    }

    @Test
    public void imageLookupNCBI() throws URISyntaxException, IOException {

    }

    @Test
    public void imageURLLookupNCBI() throws URISyntaxException, IOException {
        assertNCBIImage(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_NCBI + "28806"));
    }


    protected void assertNCBIImage(TaxonImage taxonImage) {
        assertThat(taxonImage, is(notNullValue()));
        assertThat(taxonImage.getThumbnailURL(), endsWith(".jpg"));
        assertThat(taxonImage.getImageURL(), endsWith(".jpg"));
        assertThat(taxonImage.getPageId(), is("205157"));
        assertThat(taxonImage.getInfoURL(), is("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=28806"));
        assertThat(taxonImage.getScientificName(), is("Centropomus undecimalis"));
        assertThat(taxonImage.getCommonName(), is("Snoek"));
    }

    @Test
    @Ignore
    public void imageLookupWoRMS() throws URISyntaxException, IOException {
        assertWoRMSImage(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_WORMS + "276287"));
    }

    @Test
    public void imageLookupWoRMSNoEOLPageId() throws IOException {
        TaxonImage taxonImage = imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_WORMS + "585857");
        assertThat(taxonImage.getInfoURL(), notNullValue());
    }

    private void assertWoRMSImage(TaxonImage taxonImage) {
        assertThat(taxonImage, is(notNullValue()));
        assertThat(taxonImage.getThumbnailURL(), is("http://media.eol.org/content/2009/11/17/11/81513_98_68.jpg"));
        assertThat(taxonImage.getImageURL(), is("http://media.eol.org/content/2009/11/17/11/81513_orig.jpg"));
        assertThat(taxonImage.getPageId(), is("210779"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/210779"));
        assertThat(taxonImage.getScientificName(), is("Prionotus paralatus"));
        assertThat(taxonImage.getCommonName(), is("Mexican Searobin"));
    }

    @Test
    public void imageLookupEOL() throws URISyntaxException, IOException {
        assertEOLImage(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_EOL + "1045608"));
    }

    private void assertEOLImage(TaxonImage taxonImage) {
        assertThat(taxonImage, is(notNullValue()));
        assertThat(taxonImage.getThumbnailURL(), containsString(".jpg"));
        assertThat(taxonImage.getImageURL(), containsString(".jpg"));
        assertThat(taxonImage.getPageId(), is("1045608"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/1045608"));
        assertThat(taxonImage.getScientificName(), is("Apis mellifera"));
        assertThat(taxonImage.getCommonName(), is("Western Honeybee"));
    }

    @Test
    public void imageLookupEOL2() throws URISyntaxException, IOException {
        assertEOLImage2(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_EOL + "2215"));
    }

    private void assertEOLImage2(TaxonImage taxonImage) {
        assertThat(taxonImage, is(notNullValue()));
        assertThat(taxonImage.getPageId(), is("2215"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/2215"));
        assertThat(taxonImage.getCommonName(), is("Mussels and Clams"));
        assertThat(taxonImage.getScientificName(), is("Bivalvia"));
    }

    @Test
    public void imageLookupEOL3() throws URISyntaxException, IOException {
        assertEOLImage3(imageService.lookupImageForExternalId(TaxonomyProvider.ID_PREFIX_EOL + "2774522"));
    }

    @Test
    public void imageLookupGBIF() throws URISyntaxException, IOException {
        TaxonImage taxonImage = imageService.lookupImageForExternalId("GBIF:7270064");
        assertThat(taxonImage.getPageId(), is(nullValue()));
        assertThat(taxonImage.getThumbnailURL(), is(nullValue()));
        assertThat(taxonImage.getImageURL(), is(nullValue()));
        assertThat(taxonImage.getInfoURL(), is("http://www.gbif.org/species/7270064"));
        assertThat(taxonImage.getScientificName(), is(nullValue()));
        assertThat(taxonImage.getCommonName(), is(nullValue()));
    }

    private void assertEOLImage3(TaxonImage taxonImage) {
        assertThat(taxonImage, is(notNullValue()));
        assertThat(taxonImage.getPageId(), is("2774522"));
        assertThat(taxonImage.getThumbnailURL(), containsString(".jpg"));
        assertThat(taxonImage.getImageURL(), containsString(".jpg"));
        assertThat(taxonImage.getInfoURL(), is("http://eol.org/pages/2774522"));
        assertThat(taxonImage.getScientificName(), is("Chondrichthyes"));
        assertThat(taxonImage.getCommonName(), is("Cartilaginous Fishes"));
    }
}
