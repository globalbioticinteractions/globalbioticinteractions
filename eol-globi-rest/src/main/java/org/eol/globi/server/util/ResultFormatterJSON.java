package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import sun.misc.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResultFormatterJSON implements ResultFormatterStreaming {

    @Override
    public String format(String s) throws ResultFormattingException {
        String formatted;
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(s);
            RequestHelper.throwOnError(jsonNode);
            formatted = pruneResultsIfNeeded(jsonNode)
                    .toPrettyString();
        } catch (JsonProcessingException e) {
            throw new ResultFormattingException("failed to parse", e);
        } catch (IOException e) {
            throw new ResultFormattingException("found errors in response", e);
        }
        return formatted;
    }

    private JsonNode pruneResultsIfNeeded(JsonNode jsonNode) {
        JsonNode pruned = jsonNode;
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.isArray() && results.size() == 1) {
                ObjectMapper objMapper = new ObjectMapper();
                ObjectNode objNode = objMapper.createObjectNode();
                JsonNode firstResult = results.get(0);
                objNode.set("columns", firstResult.get("columns"));
                ArrayNode rows = objMapper.createArrayNode();
                for (JsonNode rowsAndMetas : firstResult.get("data")) {
                    if (rowsAndMetas.has("row")) {
                        rows.add(rowsAndMetas.get("row"));
                    } else if (rowsAndMetas.isArray()) {
                        rows.add(rowsAndMetas);
                    }
                }
                objNode.set("data", rows);
                pruned = objNode;
            }
        }
        return pruned;
    }

    @Override
    public void format(InputStream is, OutputStream os) throws ResultFormattingException {
        try (InputStream inputStream = is) {
            // needs to be more efficient - in a streaming mapper, rather than serializing the in memory string
            JsonNode jsonNode1 = new ObjectMapper().readTree(inputStream);
            RequestHelper.throwOnError(jsonNode1);
            JsonNode jsonNode = pruneResultsIfNeeded(jsonNode1);
            IOUtils.copy(IOUtils.toInputStream(jsonNode.toPrettyString(), StandardCharsets.UTF_8), os);
            os.flush();
        } catch (IOException e) {
            throw new ResultFormattingException("failed to format incoming stream", e);
        }
    }


}
