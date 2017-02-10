package org.eol.globi.server.util;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.CSVTSVUtil;

public class ResultFormatterCSV extends ResultFormatterSeparatedValues {

    @Override
    protected void addCSVSeparator(StringBuilder resultBuilder, boolean hasNext) {
        resultBuilder.append(hasNext ? "," : "\n");
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
