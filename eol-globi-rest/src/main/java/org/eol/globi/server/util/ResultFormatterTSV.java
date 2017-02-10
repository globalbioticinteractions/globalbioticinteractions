package org.eol.globi.server.util;

import org.codehaus.jackson.JsonNode;
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
    protected void writeAsCSVCell(StringBuilder resultBuilder, JsonNode node) {
        if (!node.isNull()) {
            CSVTSVUtil.escapeTSV(resultBuilder, node);
        }
    }

}
