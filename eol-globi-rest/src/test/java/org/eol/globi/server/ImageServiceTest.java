package org.eol.globi.server;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class ImageServiceTest {

    private ImageService imageService;

    @Before
    public void init() {
        imageService = new ImageService();
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:123456");
                    }
                };
            }

            @Override
            public Map<String, String> findTaxonWithImage(String scientificName) throws IOException {
                return null;
            }

            @Override
            public Collection<String> findTaxonIds(String scientificName) throws IOException {
                return Arrays.asList("WD:123");
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
            public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
                return this.lookupImageForExternalId(externalId);
            }

        });
    }

    @Test
    public void findImagesForExternalId() throws IOException {
        TaxonImage image = imageService.findTaxonImagesForExternalId("EOL:1234", "en");
        assertThat(image.getCommonName(), is("some common name for EOL:1234"));
    }

    @Test
    public void imagesForName() throws IOException {
        assertThat(imageService.findTaxonImagesForTaxonWithName("Homo sapiens", "en"), is(notNullValue()));
    }

    @Test
    public void taxonFoundButNoImage() throws IOException {
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:123456");
                        put(PropertyAndValueDictionary.NAME, "some latin name");
                        put(PropertyAndValueDictionary.PATH, "path1 | path2");
                        put(PropertyAndValueDictionary.COMMON_NAMES, "one @en | zwei @de");
                    }
                };
            }

            @Override
            public Map<String, String> findTaxonWithImage(String scientificName) throws IOException {
                return null;
            }

            @Override
            public Collection<String> findTaxonIds(String scientificName) throws IOException {
                return Collections.singletonList("EOL:123");
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
            public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
                return null;
            }

        });

        TaxonImage image = imageService.findTaxonImagesForTaxonWithName("some name", "en");
        assertThat(image.getInfoURL(), is("http://eol.org/pages/123"));
        assertThat(image.getScientificName(), is("some latin name"));
        assertThat(image.getCommonName(), is("one"));
        assertThat(image.getTaxonPath(), is("path1 | path2"));
    }
    @Test
    public void foundImageInTaxonInfoAndInImageSearch() throws IOException {
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "EOL:123456");
                        put(PropertyAndValueDictionary.NAME, "some latin name");
                        put(PropertyAndValueDictionary.PATH, "path1 | path2");
                        put(PropertyAndValueDictionary.COMMON_NAMES, "one @en | zwei @de");
                        put(PropertyAndValueDictionary.THUMBNAIL_URL, "https://example.com");
                    }
                };
            }

            @Override
            public Map<String, String> findTaxonWithImage(String scientificName) throws IOException {
                return null;
            }

            @Override
            public Collection<String> findTaxonIds(String scientificName) throws IOException {
                return Collections.singletonList("https://www.wikidata.org/wiki/Q140");
            }
        });

        imageService.setImageSearch(new ImageSearch() {

            @Override
            public TaxonImage lookupImageForExternalId(String externalId) {
                TaxonImage taxonImage = new TaxonImage();
                taxonImage.setInfoURL("some info url");
                taxonImage.setThumbnailURL("bla");
                return taxonImage;
            }

            @Override
            public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
                return this.lookupImageForExternalId(externalId);
            }

        });

        TaxonImage image = imageService.findTaxonImagesForTaxonWithName("some name", "en");
        assertThat(image.getInfoURL(), is("some info url"));
        assertThat(image.getThumbnailURL(), is("bla"));
    }

    @Test
    public void taxonFoundButNoExternalId() throws IOException {
        imageService.setTaxonSearch(new TaxonSearch() {

            @Override
            public Map<String, String> findTaxon(String scientificName) throws IOException {
                return new HashMap<String, String>() {
                    {
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "no:match");
                        put(PropertyAndValueDictionary.NAME, "some latin name");
                    }
                };
            }

            @Override
            public Map<String, String> findTaxonWithImage(String scientificName) throws IOException {
                return null;
            }

            @Override
            public Collection<String> findTaxonIds(String scientificName) throws IOException {
                return Collections.singletonList("WD:123");
            }
        });

        imageService.setImageSearch(new ImageSearch() {

            @Override
            public TaxonImage lookupImageForExternalId(String externalId) {
                return null;
            }

            @Override
            public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
                return null;
            }

        });

        TaxonImage image = imageService.findTaxonImagesForTaxonWithName("some name", "en");
        assertThat(image.getInfoURL(), is(notNullValue()));
        assertThat(image.getScientificName(), is("some latin name"));
        assertThat(image.getCommonName(), is(nullValue()));
    }

    @Test
    public void imagesForNames() throws IOException {
        List<TaxonImage> images = imageService.findImagesForNames(new String[]{"Homo sapiens", "Ariopsis felis"}, "en");
        assertThat(images.size(), is(2));
    }

    @Test
    public void replaceFullWithPrefix() {
        Optional<String> s = ImageService.replaceFullWithPrefix(Collections.singletonList("https://www.wikidata.org/wiki/Q1390"));
        assertThat(s.get(), is("WD:Q1390"));

    }

}
