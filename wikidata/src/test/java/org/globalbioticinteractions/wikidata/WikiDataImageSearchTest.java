package org.globalbioticinteractions.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.TaxonImage;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class WikiDataImageSearchTest  {

    @Test
    public void lionImage() throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(getClass().getResourceAsStream("Q140.json"));
        TaxonImage image = new TaxonImage();
        WikiDataImageSearch.enrichWithThumbnailAndCommonNamesIfAvailable(jsonNode, "Q140", "en", image);
        assertThat(image.getScientificName(), is("Panthera leo"));
        assertThat(image.getCommonName(), Is.is("lion @en | African lion @en | the lion @en | Asiatic lion @en | Panthera leo @en"));
        assertThat(image.getThumbnailURL(), Is.is("https://commons.wikimedia.org/w/index.php?title=Special:Redirect/file/Lion_in_masai_mara.jpg&width=100"));
    }
}