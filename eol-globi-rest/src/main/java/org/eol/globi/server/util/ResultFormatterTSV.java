package org.eol.globi.server.util;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.CSVUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
            CSVUtil.escapeTSV(resultBuilder, node);
        }
    }

}
