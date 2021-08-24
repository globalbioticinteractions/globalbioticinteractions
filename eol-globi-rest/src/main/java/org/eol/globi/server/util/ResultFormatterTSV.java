package org.eol.globi.server.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.util.CSVTSVUtil;

public class ResultFormatterTSV extends ResultFormatterSeparatedValues {

    @Override
    protected void addCSVSeparator(StringBuilder resultBuilder, boolean hasNext) {
        resultBuilder.append(hasNext ? "\t" : "\n");
    }

    @Override
    protected String writeToCSVCellValue(JsonNode cell) {
        StringBuilder builder = new StringBuilder();
        writeAsCSVCell(builder, cell);
        return builder.toString();
    }

    @Override
    protected String getFieldSeparator() {
        return "\t";
    }

    @Override
    protected String getStringQuotes() {
        return "";
    }

    @Override
    protected String escapeValue(String value) {
        return CSVTSVUtil.escapeTSV(value);
    }

    @Override
    protected void writeAsCSVCell(StringBuilder resultBuilder, JsonNode node) {
        if (!node.isNull()) {
            CSVTSVUtil.escapeTSV(resultBuilder, node);
        }
    }

}
