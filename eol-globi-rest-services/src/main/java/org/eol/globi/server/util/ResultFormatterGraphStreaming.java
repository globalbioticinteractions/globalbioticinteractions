package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.TaxonUtil;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
import static org.eol.globi.domain.PropertyAndValueDictionary.NO_NAME;
import static org.eol.globi.server.util.ResultFormatterSeparatedValues.isValue;

public class ResultFormatterGraphStreaming extends ResultFormatterStreamingImpl {

    private List<String> header = new ArrayList<>();

    private AtomicLong counter = new AtomicLong(0);

    private List<String> nodeIds = new ArrayList<>();

    private RandomLong randomLong = new RandomLong() {
        private Random random = new Random();
        @Override
        public long randomLong() {
            return (long) (random.nextFloat() * 1000.0);
        }
    };

    @Override
    public String format(String s) throws ResultFormattingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        format(IOUtils.toInputStream(s, StandardCharsets.UTF_8), os);
        return StringUtils.toEncodedString(os.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    protected void handleRows(OutputStream os, JsonParser jsonParser) throws IOException {
        nodeIds.clear();
        counter.set(0);
        JsonToken token = jsonParser.nextToken();
        if (START_ARRAY.equals(token)) {
            handleStreamingJson(os, jsonParser);
        }

    }

    private void handleStreamingJson(OutputStream os, JsonParser jsonParser) throws IOException {
        JsonToken token;
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        boolean inValueArray = false;
        boolean inRowArray = false;
        String currentFieldName = null;
        Map<String, String> interaction = new TreeMap<>();

        while ((token = jsonParser.nextToken()) != null) {
            if (FIELD_NAME.equals(token)) {
                currentFieldName = jsonParser.getCurrentName();
            }
            if (START_ARRAY.equals(token)) {
                if (inRowArray && nonMetaField(currentFieldName)) {
                    inValueArray = true;
                } else {
                    inRowArray = true;
                }
            } else {
                if (isValue(token)) {
                    if (inValueArray) {
                        writeNodesAndEdgeIfAvailable(jsonParser, token, interaction, writer);
                    } else if (nonMetaField(currentFieldName)) {
                        String headerLabel = header.get(interaction.size());
                        if (StringUtils.isNotBlank(headerLabel)) {
                            String tokenValue = getTokenValue(jsonParser, token);
                            interaction.put(headerLabel, tokenValue);
                        }
                    }
                } else if (END_ARRAY.equals(token)) {
                    if (inValueArray) {
                        inValueArray = false;
                    } else if (inRowArray) {
                        if (!interaction.isEmpty()) {
                            writeNodesAndEdge(writer, interaction);
                        }
                        inRowArray = false;
                    }
                    interaction.clear();
                }
            }
        }
    }

    private boolean nonMetaField(String currentFieldName) {
        return !StringUtils.equals("meta", currentFieldName);
    }

    private void writeNodesAndEdgeIfAvailable(JsonParser jsonParser, JsonToken token, Map<String, String> interaction, BufferedWriter writer) throws IOException {
        if (!interaction.isEmpty()) {
            writeNodesAndEdge(jsonParser, token, interaction, writer);
        }
    }

    private void writeNodesAndEdge(JsonParser jsonParser, JsonToken token, Map<String, String> interaction, BufferedWriter writer) throws IOException {
        writeNodesAndEdge(writer, new TreeMap<String, String>(interaction) {{
            String headerLabel = header.get(interaction.size());
            if (StringUtils.isNotBlank(headerLabel)) {
                put(headerLabel, getTokenValue(jsonParser, token));
            }
        }});
    }

    private void writeNodesAndEdge(BufferedWriter writer, Map<String, String> interaction) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String sourceId = getId(interaction,
                ResultField.SOURCE_TAXON_NAME.getLabel(),
                interaction.get(ResultField.SOURCE_TAXON_EXTERNAL_ID.getLabel()));

        String targetId = getId(interaction,
                ResultField.TARGET_TAXON_NAME.getLabel(),
                interaction.get(ResultField.TARGET_TAXON_EXTERNAL_ID.getLabel()));

        if (StringUtils.isNotBlank(sourceId)
                && StringUtils.isNotBlank(targetId)) {
            long edgeId = counter.getAndIncrement();
            String edgeLabel = interaction.get(ResultField.INTERACTION_TYPE.getLabel());
            String predicate = edgeLabel;

            writeNodeIfNeeded(writer, objectMapper, sourceId, interaction.getOrDefault(ResultField.SOURCE_TAXON_NAME.getLabel(), sourceId));
            writeNodeIfNeeded(writer, objectMapper, targetId, interaction.getOrDefault(ResultField.TARGET_TAXON_NAME.getLabel(), targetId));

            writeEdge(writer, objectMapper, sourceId, targetId, Long.toString(edgeId), edgeLabel, predicate);
        }
    }

    private String getId(Map<String, String> interaction, String label, String id) {
        return TaxonUtil.isNonEmptyValue(id)
                ? id
                : interaction.getOrDefault(label, NO_NAME);
    }

    private void writeEdge(BufferedWriter writer, ObjectMapper objectMapper, String sourceId, String targetId, String edgeId, String edgeLabel, String predicate) throws IOException {
        ObjectNode objectNode = objectMapper.createObjectNode();
        ObjectNode edgeEntry = objectMapper.createObjectNode();
        ObjectNode edge = objectMapper.createObjectNode();
        edgeEntry.set(edgeId, edge);
        edge.set("source", new TextNode(sourceId));
        edge.set("target", new TextNode(targetId));
        edge.set("label", new TextNode(edgeLabel));
        edge.set("pred", new TextNode(predicate));

        objectNode.set("ae", edgeEntry);

        writeObject(objectNode, writer);
    }

    private void writeNodeIfNeeded(Writer writer, ObjectMapper objectMapper, String id, String label) throws IOException {
        if (!nodeIds.contains(id)) {
            writeNode(writer, objectMapper, id, label);
            nodeIds.add(id);
        }
    }

    private void writeNode(Writer writer, ObjectMapper objectMapper, String id, String label) throws IOException {
        ObjectNode objectNode = objectMapper.createObjectNode();
        ObjectNode nodeEntry = objectMapper.createObjectNode();
        ObjectNode sourceNode = objectMapper.createObjectNode();
        sourceNode.set("label", new TextNode(label));

//        sourceNode.set("size", new FloatNode(1));
        sourceNode.set("x", new LongNode(getRandomLong().randomLong()));
        sourceNode.set("y", new LongNode(getRandomLong().randomLong()));
        nodeEntry.set(id, sourceNode);
        objectNode.set("an", nodeEntry);
        writeObject(objectNode, writer);
    }

    private void writeObject(ObjectNode objectNode, Writer writer) throws IOException {
        writer.write(objectNode.toString());
        writer.write("\r\n");
        writer.flush();
    }

    @Override
    protected void handleHeader(OutputStream os, JsonParser jsonParser) throws IOException {
        header.clear();
        JsonToken token;
        token = jsonParser.nextToken();
        if (START_ARRAY.equals(token)) {
            while ((token = jsonParser.nextToken()) != null && !END_ARRAY.equals(token)) {
                header.add(getTokenValue(jsonParser, token));
            }
        }
    }

    private String getTokenValue(JsonParser jsonParser, JsonToken token) throws IOException {
        String text = "";
        if (isValue(token)) {
            text = VALUE_NULL.equals(token) ? "" : jsonParser.getText();
        }
        return text;
    }

    public RandomLong getRandomLong() {
        return randomLong;
    }

    public void setRandomLong(RandomLong randomLong) {
        this.randomLong = randomLong;
    }


    interface RandomLong {
        long randomLong();
    }





}
