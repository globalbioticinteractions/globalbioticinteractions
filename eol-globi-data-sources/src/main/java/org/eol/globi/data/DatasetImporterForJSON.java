package org.eol.globi.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;

public class DatasetImporterForJSON extends DatasetImporterWithListener {

    public String getBaseUrl() {
        return getDataset().getArchiveURI().toString();
    }

    public DatasetImporterForJSON(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            importRepository(getSourceCitation());
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        }
    }


    private void importRepository(String sourceCitation) throws IOException, StudyImporterException {
        ArrayList<IOException> parserExceptions = new ArrayList<>();
        URI resourceURI = URI.create("/interactions.json");
        InputStream is = getDataset().retrieve(resourceURI);
        if (is == null) {
            throw new StudyImporterException("failed to access [" + "/interactions.json" + "] as individual resource (e.g. local/remote data/file).");
        } else {
            final InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            final BufferedReader bufferedReader = IOUtils.toBufferedReader(reader);
            bufferedReader.lines().forEach(line -> {
                try {
                    JsonNode jsonNode = new ObjectMapper().readTree(line);
                    final Map<String, String> interaction = new TreeMap<>();
                    InteractUtil.putNotBlank(interaction, DATASET_CITATION, CitationUtil.sourceCitationLastAccessed(getDataset(), sourceCitation == null ? "" : sourceCitation + ". "));
                    if (jsonNode.has("referenceId")) {
                        InteractUtil.putNotBlank(interaction, REFERENCE_ID, jsonNode.get("referenceId").asText());
                    }
                    jsonNode.fields().forEachRemaining(entry -> {
                        if (entry.getValue().isValueNode()) {
                            interaction.put(entry.getKey(), entry.getValue().asText());
                        }
                    });
                    getInteractionListener().on(interaction);
                } catch (StudyImporterException e) {
                    ((List<IOException>) parserExceptions).add(new IOException("failed to import [" + resourceURI.toString() + "]", e));
                } catch (JsonProcessingException e) {
                    ((List<IOException>) parserExceptions).add(new IOException("failed to parse [" + resourceURI.toString() + "] - is the resource in JSON Lines format https://jsonlines.org/ ?", e));
                }
            });
        }

        if (parserExceptions.size() > 0) {
            throw new IOException("failed some, or all of, attempts to index [/interactions.json]", parserExceptions.get(0));
        }

    }

}
