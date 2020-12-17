package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.process.InteractionListener;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class TableInteractionListenerProxyTest {

    @Test
    public void interactionListenerTest() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        JsonNode config = new ObjectMapper().readTree("{ \"dcterms:bibliographicCitation\":\"some citation\", \"url\":\"https://example.org/someResource\" }");
        DatasetImpl dataset = new DatasetImpl(null, URI.create("http://someurl"), inStream -> inStream);
        dataset.setConfig(config);
        final TableInteractionListenerProxy listener = new TableInteractionListenerProxy(dataset, new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
            }
        });
        listener.on(new HashMap<>());

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(DatasetImporterForTSV.DATASET_CITATION), startsWith("some citation. Accessed at <https://example.org/someResource> on "));
        assertThat(links.get(0).get(DatasetImporterForTSV.REFERENCE_CITATION), startsWith("some citation. Accessed at <https://example.org/someResource> on "));
    }

    @Test
    public void interactionListener2Test() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        JsonNode config = new ObjectMapper().readTree("{ \"dcterms:bibliographicCitation\":\"some citation\", \"url\":\"https://example.org/someResource\" }");
        DatasetImpl dataset = new DatasetImpl(null, URI.create("http://someurl"), inStream -> inStream);
        dataset.setConfig(config);
        final TableInteractionListenerProxy listener = new TableInteractionListenerProxy(dataset, new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                links.add(interaction);
            }
        });
        listener.on(new HashMap<String, String>() {
            {
                put(DatasetImporterForTSV.REFERENCE_CITATION, "some ref");
            }
        });

        assertThat(links.size(), is(1));
        assertThat(links.get(0).get(DatasetImporterForTSV.DATASET_CITATION), startsWith("some citation. Accessed at <https://example.org/someResource> on "));
        assertThat(links.get(0).get(DatasetImporterForTSV.REFERENCE_CITATION), startsWith("some ref"));
    }



}