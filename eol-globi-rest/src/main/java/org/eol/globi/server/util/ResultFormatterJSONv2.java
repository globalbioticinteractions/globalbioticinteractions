package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        JsonNode jsonNode;
        try {
            jsonNode = RequestHelper.parse(result);
        } catch (IOException e) {
            throw new ResultFormattingException("cannot for an error message", e);
        }

        try {
            RequestHelper.throwOnError(jsonNode);
        } catch (IOException e) {
            throw new ResultFormattingException("result contains errors", e);
        }
        return format(jsonNode);

    }

    private String format(JsonNode resultNode) throws ResultFormattingException {
        List<String> columnNames = new ArrayList<String>();

        JsonNode jsonNode = RequestHelper.getFirstResult(resultNode);

        JsonNode columns = jsonNode.get("columns");
        if (columns == null) {
            throw new IllegalArgumentException("columns array expected, but not found");
        }
        for (JsonNode column : columns) {
            columnNames.add(column.asText());
        }

        JsonNode rowsAndMetas = jsonNode.get("data");
        if (rowsAndMetas == null) {
            throw new IllegalArgumentException("data array expected, but none found");
        }

        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        if (isInteractionQuery(columnNames)) {
            for (JsonNode rowAndMeta : rowsAndMetas) {
                Map<String, Object> interaction = new HashMap<String, Object>();

                Map<String, String> sourceTaxon = new HashMap<String, String>();
                interaction.put("source", sourceTaxon);

                Map<String, String> targetTaxon = new HashMap<String, String>();
                interaction.put("target", targetTaxon);

                List<Map<String, String>> targetTaxa = new ArrayList<>();


                JsonNode row = RequestHelper.getRow(rowAndMeta);
                if (!row.isArray()) {
                    throw new ResultFormattingException("expected row value array, but none found in [" + rowAndMeta.toString() + "]");
                }

                if (row.size() != columnNames.size()) {
                    throw new ResultFormattingException("number of values and columns do not match for [" + rowAndMeta.toString() + "]");
                }

                for (int i = 0; i < row.size(); i++) {
                    parseRow(columnNames, row, interaction, sourceTaxon, targetTaxon, targetTaxa, i);
                }

                if (targetTaxa.size() > 0) {
                    for (Map<String, String> aTargetTaxon : targetTaxa) {
                        Map<String, Object> anotherInteraction = new HashMap<String, Object>();
                        resultList.add(anotherInteraction);
                        anotherInteraction.putAll(interaction);
                        anotherInteraction.put("target", aTargetTaxon);
                    }
                } else {
                    resultList.add(interaction);
                }
            }
        } else if (isTaxonQuery(columnNames)) {
            for (JsonNode rowAndMeta : rowsAndMetas) {
                JsonNode row = RequestHelper.getRow(rowAndMeta);
                Map<String, Object> taxon = new TreeMap<String, Object>();
                for (int i = 0; i < row.size(); i++) {
                    taxon.put(columns.get(i).asText(), row.get(i).asText());
                }
                resultList.add(taxon);
            }
        }
        addAllDataColumns(jsonNode, columnNames, resultList);

        try {
            return new ObjectMapper().writeValueAsString(resultList);
        } catch (JsonProcessingException e) {
            throw new ResultFormattingException("failed to format result", e);
        }
    }

    private void addAllDataColumns(JsonNode jsonNode, List<String> columnNames, List<Map<String, Object>> interactions) {
        JsonNode rowsAndMetas = jsonNode.get("data");
        for (int j = 0; j < rowsAndMetas.size(); j++) {
            Map<String, Object> values = new HashMap<String, Object>();
            JsonNode rowAndMeta = rowsAndMetas.get(j);
            JsonNode row = RequestHelper.getRow(rowAndMeta);
            for (int k = 0; k < row.size(); k++) {
                values.put(columnNames.get(k), row.get(k));
            }
            if (interactions.size() <= j) {
                interactions.add(values);
            } else {
                interactions.get(j).putAll(values);
            }
        }
    }

    private boolean isInteractionQuery(List<String> columnNames) {
        return columnNames.contains(ResultField.INTERACTION_TYPE.getLabel());
    }

    private boolean isTaxonQuery(List<String> columnNames) {
        for (ResultField resultField : CypherQueryBuilder.TAXON_FIELDS) {
            if (columnNames.contains(resultField.getLabel())) {
                return true;
            }
        }
        return false;
    }

    private void parseRow(List<String> columnNames,
                          JsonNode row,
                          Map<String, Object> interaction,
                          Map<String, String> sourceTaxon,
                          Map<String, String> targetTaxon,
                          List<Map<String, String>> targetTaxa,
                          int i) {
        String colName = columnNames.get(i);
        final JsonNode value = row.get(i);
        if (ResultField.INTERACTION_TYPE.getLabel().equals(colName)) {
            interaction.put("type", value.asText());
        } else if (ResultField.SOURCE_TAXON_NAME.getLabel().equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("name", value.asText());
            }
        } else if (ResultField.SOURCE_TAXON_PATH.getLabel().equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("path", value.asText());
            }
        } else if (ResultField.SOURCE_TAXON_EXTERNAL_ID.getLabel().equals(colName)) {
            if (value.isTextual()) {
                sourceTaxon.put("id", value.asText());
            }
        } else if (ResultField.TARGET_TAXON_NAME.getLabel().equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("name", value.asText());
            } else if (value.isArray()) {
                for (final JsonNode name : value) {
                    if (name.isTextual()) {
                        addTargetTaxon(targetTaxa, name);
                    }
                }
            }
        } else if (ResultField.TARGET_TAXON_PATH.getLabel().equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("path", value.asText());
            }
        } else if (ResultField.TARGET_TAXON_EXTERNAL_ID.getLabel().equals(colName)) {
            if (value.isTextual()) {
                targetTaxon.put("id", value.asText());
            }
        } else if (ResultField.LATITUDE.getLabel().equals(colName)) {
            if (value.isNumber()) {
                interaction.put("latitude", value.asDouble());
            }
        } else if (ResultField.LONGITUDE.getLabel().equals(colName)) {
            if (value.isNumber()) {
                interaction.put("longitude", value.asDouble());
            }
        } else if (ResultField.ALTITUDE.getLabel().equals(colName)) {
            if (value.isNumber()) {
                interaction.put("altitude", value.asDouble());
            }
        } else if (ResultField.EVENT_DATE.getLabel().equals(colName)) {
            if (value.isNumber()) {
                interaction.put("time", value.asText());
            }
        } else if (ResultField.STUDY_TITLE.getLabel().equals(colName)) {
            interaction.put("study", value.asText());
        }
    }

    private void addTargetTaxon(List<Map<String, String>> targetTaxa, final JsonNode name) {
        targetTaxa.add(new HashMap<String, String>() {{
            put("name", name.asText());
        }});
    }
}
