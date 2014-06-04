package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ImageServiceTest {
    @Test
    public void findImagesForExternalId() throws IOException {
        TaxonImage image = new ImageService().findTaxonImagesForExternalId("EOL:276287");
        assertThat(image.getCommonName(), is(nullValue()));
        assertThat(image.getThumbnailURL(), is("http://media.eol.org/content/2011/12/13/21/66989_98_68.jpg"));
        assertThat(image.getImageURL(), is("http://media.eol.org/content/2011/12/13/21/66989_orig.jpg"));
        assertThat(image.getInfoURL(), is("http://eol.org/pages/276287"));
        assertThat(image.getEOLPageId(), is("276287"));
        assertThat(image.getScientificName(), is("Oospila albicoma"));
    }

    @Test
    public void imagesForName() throws IOException {
        TaxonImage image = new ImageService().findTaxonImagesForTaxonWithName("Homo sapiens");
        assertThat(image, is(notNullValue()));
    }

    @Test
    public void imagesForNames() throws IOException {
        List<TaxonImage> images = new ImageService().findImagesForNames(new String[]{"Homo sapiens", "Ariopsis felis"});
        assertThat(images.size(), is(2)) ;
    }

}
