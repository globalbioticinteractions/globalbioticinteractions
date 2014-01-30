package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultFormatterJSONv2 implements ResultFormatter {

    @Override
    public String format(String result) throws ResultFormattingException {
        try {
            JsonNode jsonNode = CypherQueryExecutor.parse(result);
            return format(jsonNode);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to format result", e);
        }
    }

    private String format(JsonNode jsonNode) throws IOException {
        List<String> columnNames = new ArrayList<String>();

        JsonNode columns = jsonNode.get("columns");
        if (columns == null) {
            throw new IllegalArgumentException("columns array expected, but not found");
        }
        for (JsonNode column : columns) {
            columnNames.add(column.getValueAsText());
        }

        JsonNode data = jsonNode.get("data");
        if (data == null) {
            throw new IllegalArgumentException("data array expected, but not found");
        }

        List<Map<String, Object>> interactions = new ArrayList<Map<String, Object>>();


        for (JsonNode row : data) {
            Map<String, Object> interaction = new HashMap<String, Object>();

            Map<String, String> sourceTaxon = new HashMap<String, String>();
            interaction.put("source", sourceTaxon);

            Map<String, String> targetTaxon = new HashMap<String, String>();
            interaction.put("target", targetTaxon);

            List<Map<String, String>> targetTaxa = new ArrayList<Map<String, String>>();

            for (int i = 0; i < row.size(); i++) {
                parseRow(columnNames, row, interaction, sourceTaxon, targetTaxon, targetTaxa, i);
            }

            if (targetTaxa.size() > 0) {
                for (Map<String, String> aTargetTaxon : targetTaxa) {
                    Map<String, Object> anotherInteraction = new HashMap<String, Object>();
                    interactions.add(anotherInteraction);
                    anotherInteraction.putAll(interaction);
                    anotherInteraction.put("target", aTargetTaxon);
                }
            } else {
                interactions.add(interaction);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(interactions);
    }

    private void parseRow(List<String> columnNames, JsonNode row, Map<String, Object> interaction, Map<String, String> sourceTaxon, Map<String, String> targetTaxon, List<Map<String, String>> targetTaxa, int i) {
        String colName = columnNames.get(i);
        final JsonNode value = row.get(i);
        if (ResultFields.INTERACTION_TYPE.equals(colName)) {
            interaction.put("type", value.getValueAsText());
        } else if (ResultFields.SOURCE_TAXON_NAME.equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("name", value.getValueAsText());
            }
        } else if (ResultFields.SOURCE_TAXON_PATH.equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("path", value.getValueAsText());
            }
        } else if (ResultFields.SOURCE_TAXON_EXTERNAL_ID.equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("id", value.getValueAsText());
            }
        } else if (ResultFields.TARGET_TAXON_NAME.equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("name", value.getValueAsText());
            } else if (value.isArray()) {
                for (final JsonNode name : value) {
                    if (name.isTextual()) {
                        addTargetTaxon(targetTaxa, name);
                    }
                }
            }
        } else if (ResultFields.TARGET_TAXON_PATH.equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("path", value.getValueAsText());
            }
        } else if (ResultFields.TARGET_TAXON_EXTERNAL_ID.equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("id", value.getValueAsText());
            }
        } else if (ResultFields.LATITUDE.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("latitude", value.getValueAsDouble());
            }
        } else if (ResultFields.LONGITUDE.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("longitude", value.getValueAsDouble());
            }
        } else if (ResultFields.ALTITUDE.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("altitude", value.getValueAsDouble());
            }
        } else if (ResultFields.COLLECTION_TIME_IN_UNIX_EPOCH.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("time", value.getValueAsLong());
            }
        } else if (ResultFields.STUDY_TITLE.equals(colName)) {
            interaction.put("study", value.getValueAsText());
        }
    }

    private void addTargetTaxon(List<Map<String, String>> targetTaxa, final JsonNode name) {
        targetTaxa.add(new HashMap<String, String>() {{
            put("name", name.getValueAsText());
        }});
    }
}
