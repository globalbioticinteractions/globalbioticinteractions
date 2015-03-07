package org.eol.globi.server.util;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.server.CypherQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ResultFormatterJSONv2 implements ResultFormatter {

    @Override
    public String format(String result) throws ResultFormattingException {
        try {
            JsonNode jsonNode = RequestHelper.parse(result);
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
            columnNames.add(column.getTextValue());
        }

        JsonNode data = jsonNode.get("data");
        if (data == null) {
            throw new IllegalArgumentException("data array expected, but not found");
        }

        if (isInteractionQuery(columnNames)) {
            return formatAsSourceTargetPairs(columnNames, data);
        } else if (isTaxonQuery(columnNames)) {
            List<Map<String, String>> taxa = new ArrayList<Map<String, String>>();
            for (JsonNode row : data) {
                Map<String, String> taxon = new TreeMap<String, String>();
                for (int i = 0; i < row.size(); i++) {
                    taxon.put(columns.get(i).asText(), row.get(i).asText());
                }
                taxa.add(taxon);
            }
            return new ObjectMapper().writeValueAsString(taxa);
        }

        return "[]";
    }

    private boolean isInteractionQuery(List<String> columnNames) {
        return columnNames.contains(ResultField.INTERACTION_TYPE);
    }

    private boolean isTaxonQuery(List<String> columnNames) {
        return CollectionUtils.containsAny(CypherQueryBuilder.TAXON_FIELDS, columnNames);
    }

    private String formatAsSourceTargetPairs(List<String> columnNames, JsonNode data) throws IOException {
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
        if (ResultField.INTERACTION_TYPE.equals(colName)) {
            interaction.put("type", value.getTextValue());
        } else if (ResultField.SOURCE_TAXON_NAME.equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("name", value.getTextValue());
            }
        } else if (ResultField.SOURCE_TAXON_PATH.equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("path", value.getTextValue());
            }
        } else if (ResultField.SOURCE_TAXON_EXTERNAL_ID.equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("id", value.getTextValue());
            }
        } else if (ResultField.TARGET_TAXON_NAME.equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("name", value.getTextValue());
            } else if (value.isArray()) {
                for (final JsonNode name : value) {
                    if (name.isTextual()) {
                        addTargetTaxon(targetTaxa, name);
                    }
                }
            }
        } else if (ResultField.TARGET_TAXON_PATH.equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("path", value.getTextValue());
            }
        } else if (ResultField.TARGET_TAXON_EXTERNAL_ID.equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("id", value.getTextValue());
            }
        } else if (ResultField.LATITUDE.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("latitude", value.getDoubleValue());
            }
        } else if (ResultField.LONGITUDE.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("longitude", value.getDoubleValue());
            }
        } else if (ResultField.ALTITUDE.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("altitude", value.getDoubleValue());
            }
        } else if (ResultField.COLLECTION_TIME_IN_UNIX_EPOCH.equals(colName)) {
            if (value.isNumber()) {
                interaction.put("time", value.getLongValue());
            }
        } else if (ResultField.STUDY_TITLE.equals(colName)) {
            interaction.put("study", value.getTextValue());
        }
    }

    private void addTargetTaxon(List<Map<String, String>> targetTaxa, final JsonNode name) {
        targetTaxa.add(new HashMap<String, String>() {{
            put("name", name.getTextValue());
        }});
    }
}
