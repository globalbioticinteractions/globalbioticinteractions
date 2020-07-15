package org.eol.globi.data;

import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;
import static org.eol.globi.domain.InteractType.HOST_OF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

public class StudyImporterForZenodoMetadataTest {

    @Test
    public void findAnnotationsStatic() throws IOException, StudyImporterException {
        final InputStream searchResultStream = getClass().getResourceAsStream("zenodo/search-results.json");
        assertInteractionOfExamplePub(searchResultStream);
    }

    public static void assertInteractionOfExamplePub(InputStream searchResultStream) throws IOException, StudyImporterException {
        List<Map<String, String>> links = new ArrayList<>();
        final InteractionListener interactionListener = new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                links.add(link);
            }
        };


        StudyImporterForZenodoMetadata.parseSearchResults(searchResultStream, interactionListener);

        assertThat(links.size(), Is.is(12));
        final Map<String, String> first = links.get(0);
        assertRefenceAndInteractionType(first);
        assertThat(first, hasEntry(TaxonUtil.SOURCE_TAXON_NAME, "Rhinolophus"));
        assertThat(first, hasEntry(TaxonUtil.TARGET_TAXON_NAME, "Severe acute respiratory syndrome coronavirus"));

        final Map<String, String> last = links.get(11);
        assertThat(last, hasEntry(TaxonUtil.SOURCE_TAXON_NAME, "Rhinolophus ferrumequinum"));
        assertThat(last, hasEntry(TaxonUtil.TARGET_TAXON_NAME, "SL-CoV Rp3"));
    }

    public static void assertRefenceAndInteractionType(Map<String, String> first) {
        assertThat(first, hasEntry(INTERACTION_TYPE_ID, HOST_OF.getIRI()));
        assertThat(first, hasEntry(INTERACTION_TYPE_NAME, HOST_OF.getLabel()));
        assertThat(first, hasEntry(REFERENCE_CITATION, "Wendong Li. (2005). Bats Are Natural Reservoirs of SARS-like Coronaviruses. Zenodo. https://doi.org/10.1234/testing-covid-rels"));
        assertThat(first, hasEntry(REFERENCE_DOI, "10.1234/testing-covid-rels"));
        assertThat(first, hasEntry(REFERENCE_ID, "10.1234/testing-covid-rels"));
        assertThat(first, hasEntry(REFERENCE_URL, "https://doi.org/10.1234/testing-covid-rels"));
    }

}