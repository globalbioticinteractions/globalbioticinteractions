package org.eol.globi.data;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;
import static org.eol.globi.domain.InteractType.typeOf;

public class StudyImporterForZenodoMetadata extends StudyImporterWithListener {

    public StudyImporterForZenodoMetadata(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        final String zenodoSearchURI = getDataset()
                .getOrDefault("url", "https://zenodo.org/api/records/?custom=%5Bobo%3ARO_0002453%5D%3A%5B%3A%5D");
        URI uri = URI.create(zenodoSearchURI);
        while (uri != null) {
            try (InputStream is = getDataset().retrieve(uri)) {
                uri = parseSearchResults(is, getInteractionListener());
            } catch (IOException e) {
                throw new StudyImporterException("failed to access Zenodo api", e);
            }
        }
    }


    public static URI parseSearchResults(InputStream searchResultStream, InteractionListener interactionListener) throws IOException, StudyImporterException {
        final JsonNode searchResults = new ObjectMapper().readTree(searchResultStream);
        final PubListener recordListener = new MyPubListener(interactionListener);

        if (searchResults.has("hits")) {
            final JsonNode hits = searchResults.get("hits");
            if (hits.has("hits")) {
                JsonNode hitsArray = hits.get("hits");
                for (JsonNode hit : hitsArray) {
                    recordListener.onRecord(hit);
                }
            }
        }

        URI nextPage = null;
        if (searchResults.has("links")) {
            final JsonNode links = searchResults.get("links");
            if (links.has("next")) {
                final JsonNode next = links.get("next");
                nextPage = URI.create(next.asText());
            }
        }
        return nextPage;
    }

    interface PubListener {
        void onRecord(JsonNode pub) throws StudyImporterException;
    }

    private static class MyPubListener implements PubListener {
        private final InteractionListener listener;

        MyPubListener(InteractionListener interactionListener) {
            this.listener = interactionListener;
        }

        @Override
        public void onRecord(JsonNode hit) throws StudyImporterException {
            final String doi = hit.get("doi").asText();
            final JsonNode metadata = hit.get("metadata");
            String publicationYear = metadata.get("publication_date").asText().split("-")[0];
            final JsonNode creators = metadata.get("creators");
            List<String> creatorNames = new ArrayList<>();
            for (JsonNode creator : creators) {
                final String[] creatorName = creator.get("name").asText().split(",");
                ArrayUtils.reverse(creatorName);
                creatorNames.add(StringUtils.trim(StringUtils.join(creatorName, " ")));
            }
            final String title = metadata.get("title").asText();

            final DOI doi1;
            try {
                doi1 = DOI.create(doi);
            } catch (MalformedDOIException e) {
                throw new StudyImporterException("malformed doi", e);
            }
            String citation = StringUtils.join(Arrays.asList(StringUtils.join(creatorNames, ","), "(" + publicationYear + ")", title, "Zenodo", doi1.toURI().toString()), ". ");

            final JsonNode custom = metadata.get("custom");
            final Iterator<String> fieldNames = custom.getFieldNames();
            final Spliterator<String> stringSpliterator = Spliterators
                    .spliteratorUnknownSize(fieldNames, Spliterator.ORDERED);
            final Stream<String> fields = StreamSupport.stream(stringSpliterator, false);

            final Stream<Triple<String, InteractType, String>> interactions = fields
                    .map(x -> Pair.of(x, StringUtils.replace(x, "obo:RO_", PropertyAndValueDictionary.RO_NAMESPACE)))
                    .map(x -> Pair.of(x.getKey(), typeOf(x.getValue())))
                    .filter(x -> x.getValue() != null)
                    .flatMap(x -> {
                        final JsonNode annotations = custom.get(x.getKey());
                        final Stream.Builder<Triple<String, InteractType, String>> builder = Stream.builder();
                        for (JsonNode annotation : annotations) {
                            final JsonNode subjects = annotation.get("subject");
                            for (JsonNode subject : subjects) {
                                final JsonNode objects = annotation.get("object");
                                for (JsonNode object : objects) {
                                    builder.add(Triple.of(subject.asText(), x.getValue(), object.asText()));
                                }
                            }
                        }
                        return builder.build();
                    });


            final Stream<TreeMap<String, String>> links = interactions
                    .map(interaction -> new TreeMap<String, String>() {{
                        put(INTERACTION_TYPE_NAME, interaction.getMiddle().getLabel());
                        put(INTERACTION_TYPE_ID, interaction.getMiddle().getIRI());
                        put(REFERENCE_DOI, doi);
                        put(REFERENCE_URL, doi1.toURI().toString());
                        put(REFERENCE_CITATION, citation);
                        put(REFERENCE_ID, doi);
                        put(TaxonUtil.SOURCE_TAXON_NAME, interaction.getLeft());
                        put(TaxonUtil.TARGET_TAXON_NAME, interaction.getRight());
                    }});

            final Iterator<TreeMap<String, String>> iter = links.iterator();
            while (iter.hasNext()) {
                listener.newLink(iter.next());
            }
        }
    }
}
