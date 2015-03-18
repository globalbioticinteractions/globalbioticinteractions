package org.eol.globi.server;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ImageServiceTest {

    private ImageService imageService;

    @Before
    public void init() {
        imageService = new ImageService();
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName, HttpServletRequest request) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:123456");
                    }
                };
            }
        });

        imageService.setImageSearch(new ImageSearch() {

            @Override
            public TaxonImage lookupImageForExternalId(String externalId) {
                TaxonImage taxonImage = new TaxonImage();
                taxonImage.setCommonName("some common name for " + externalId);
                return taxonImage;
            }

            @Override
            public TaxonImage lookupImageURLs(TaxonomyProvider provider, String taxonId) throws IOException {
                return null;
            }
        });
    }

    @Test
    public void findImagesForExternalId() throws IOException {
        TaxonImage image = imageService.findTaxonImagesForExternalId("EOL:1234");
        assertThat(image.getCommonName(), is("some common name for EOL:1234"));
    }

    @Test
    public void imagesForName() throws IOException {
        TaxonImage image = imageService.findTaxonImagesForTaxonWithName("Homo sapiens");
        assertThat(image, is(notNullValue()));
    }

    @Test
    public void taxonFoundButNoImage() throws IOException {
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName, HttpServletRequest request) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:123456");
                        put(PropertyAndValueDictionary.NAME, "some latin name");
                        put(PropertyAndValueDictionary.COMMON_NAMES, "one @en | zwei @de");
                    }
                };
            }
        });

        imageService.setImageSearch(new ImageSearch() {

            @Override
            public TaxonImage lookupImageForExternalId(String externalId) {
                TaxonImage taxonImage = new TaxonImage();
                taxonImage.setInfoURL("some info url");
                return taxonImage;
            }

            @Override
            public TaxonImage lookupImageURLs(TaxonomyProvider provider, String taxonId) throws IOException {
                return null;
            }
        });

        TaxonImage image = imageService.findTaxonImagesForTaxonWithName("some name");
        assertThat(image.getInfoURL(), is("some info url"));
        assertThat(image.getScientificName(), is("some latin name"));
        assertThat(image.getCommonName(), is("one"));
    }

    @Test
    public void taxonFoundButNoExternalId() throws IOException {
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName, HttpServletRequest request) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "no:match");
                        put(PropertyAndValueDictionary.NAME, "some latin name");
                    }
                };
            }
        });

        imageService.setImageSearch(new ImageSearch() {

            @Override
            public TaxonImage lookupImageForExternalId(String externalId) {
                return null;
            }

            @Override
            public TaxonImage lookupImageURLs(TaxonomyProvider provider, String taxonId) throws IOException {
                return null;
            }
        });

        TaxonImage image = imageService.findTaxonImagesForTaxonWithName("some name");
        assertThat(image.getInfoURL(), is(nullValue()));
        assertThat(image.getScientificName(), is("some latin name"));
        assertThat(image.getCommonName(), is(nullValue()));
    }

    @Test
    public void imagesForNames() throws IOException {
        List<TaxonImage> images = imageService.findImagesForNames(new String[]{"Homo sapiens", "Ariopsis felis"});
        assertThat(images.size(), is(2));
    }

}
