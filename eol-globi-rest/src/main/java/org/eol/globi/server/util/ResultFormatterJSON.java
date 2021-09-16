package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResultFormatterJSON implements ResultFormatterStreaming {

    @Override
    public String format(String s) throws ResultFormattingException {
        String formatted = s;
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(s);
            formatted = pruneResultsIfNeeded(jsonNode)
                    .toPrettyString();
        } catch (JsonProcessingException e) {
            throw new ResultFormattingException("failed to parse", e);
        }
        return formatted;
    }

    private JsonNode pruneResultsIfNeeded(JsonNode jsonNode) {
        JsonNode pruned = jsonNode;
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.isArray() && results.size() == 1) {
                pruned = results.get(0);
            }
        }
        return pruned;
    }

    @Override
    public void format(InputStream is, OutputStream os) throws ResultFormattingException {
        try (InputStream inputStream = is) {
            // needs to be more efficient - in a streaming mapper, rather than serializing the in memory string
            JsonNode jsonNode = pruneResultsIfNeeded(new ObjectMapper().readTree(inputStream));
            IOUtils.copy(IOUtils.toInputStream(jsonNode.toPrettyString(), StandardCharsets.UTF_8), os);
            os.flush();
        } catch (IOException e) {
            throw new ResultFormattingException("failed to format incoming stream", e);
        }
    }


}
