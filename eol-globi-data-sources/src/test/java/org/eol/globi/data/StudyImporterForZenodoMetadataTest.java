package org.eol.globi.data;

import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertNotNull;
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

    @Test
    public void findAnnotationsStaticZenodo() throws IOException, StudyImporterException {
        final InputStream searchResultStream = getClass().getResourceAsStream("zenodo/search-results-zenodo.json");
        List<Map<String, String>> links = new ArrayList<>();
        final InteractionListener interactionListener = new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
                links.add(link);
            }
        };


        StudyImporterForZenodoMetadata.parseSearchResults(searchResultStream, interactionListener);

        assertThat(links.size(), Is.is(97));

        List<String> citations = new ArrayList<>();
        for (Map<String, String> link : links) {
            final String s = link.get(REFERENCE_CITATION);
            assertNotNull(s);
            citations.add(s);
        }

        assertThat(citations.get(0), Is.is("Rachel L. Graham, Ralph S. Baric. (2010). Recombination, Reservoirs, and the Modular Spike: Mechanisms of Coronavirus Cross-Species Transmission. Journal of Virology. https://doi.org/10.1128/JVI.01394-09"));
    }

    @Test
    public void paginate() throws IOException, StudyImporterException {

        AtomicInteger counter = new AtomicInteger(0);
        final StudyImporterForZenodoMetadata studyImporterForZenodoMetadata = new StudyImporterForZenodoMetadata(null, null);
        studyImporterForZenodoMetadata.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> link) throws StudyImporterException {
            }
        });


        final DatasetImpl dataset = new DatasetImpl("name/space", URI.create("some:uri"), in -> in) {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                counter.incrementAndGet();
                InputStream is = null;
                if (URI.create("https://sandbox.zenodo.org/api/records/?sort=mostrecent&custom=%5Bobo%3ARO_0002453%5D%3A%5B%3A%5D&page=2&size=10")
                        .equals(resourceName)) {
                    is = StudyImporterForZenodoMetadataTest.class.getResourceAsStream("zenodo/search-results-page-2.json");
                } else if (URI.create("https://zenodo.org/api/records/?custom=%5Bobo%3ARO_0002453%5D%3A%5B%3A%5D")
                        .equals(resourceName)) {
                    is = StudyImporterForZenodoMetadataTest.class.getResourceAsStream("zenodo/search-results-page-1.json");
                } else {
                    throw new IOException("kaboom!");
                }
                return is;
            }
        };
        studyImporterForZenodoMetadata.setDataset(dataset);

        studyImporterForZenodoMetadata.importStudy();

        assertThat(counter.get(), Is.is(2));
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

    private static void assertRefenceAndInteractionType(Map<String, String> first) {
        assertThat(first, hasEntry(INTERACTION_TYPE_ID, HOST_OF.getIRI()));
        assertThat(first, hasEntry(INTERACTION_TYPE_NAME, HOST_OF.getLabel()));
        assertThat(first, hasEntry(REFERENCE_CITATION, "Wendong Li. (2005). Bats Are Natural Reservoirs of SARS-like Coronaviruses. https://doi.org/10.1234/testing-covid-rels"));
        assertThat(first, hasEntry(REFERENCE_DOI, "10.1234/testing-covid-rels"));
        assertThat(first, hasEntry(REFERENCE_ID, "10.1234/testing-covid-rels"));
        assertThat(first, hasEntry(REFERENCE_URL, "https://doi.org/10.1234/testing-covid-rels"));
    }

}