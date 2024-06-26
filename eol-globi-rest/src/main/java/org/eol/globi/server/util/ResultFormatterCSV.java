package org.eol.globi.server.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.util.CSVTSVUtil;

public class ResultFormatterCSV extends ResultFormatterSeparatedValues {

    @Override
    protected void addCSVSeparator(StringBuilder resultBuilder, boolean hasNext) {
        resultBuilder.append(hasNext ? "," : "\n");
    }

    @Override
    protected String getFieldSeparator() {
        return ",";
    }

    @Override
    protected String getStringQuotes() {
        return "\"";
    }

    @Override
    protected String escapeValue(String value) {
        return CSVTSVUtil.escapeCSV(value);
    }

    @Override
    protected String writeToCSVCellValue(JsonNode cell) {
        StringBuilder builder = new StringBuilder();
        writeAsCSVCell(builder, cell);
        return builder.toString();
    }

    @Override
    protected void writeAsCSVCell(StringBuilder resultBuilder, JsonNode node) {
        if (!node.isNull()) {
            if (node.isTextual()) {
                resultBuilder.append("\"");
            }
            CSVTSVUtil.escapeQuotes(resultBuilder, node);
            if (node.isTextual()) {
                resultBuilder.append("\"");
            }
        }
    }

}
