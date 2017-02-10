package org.eol.globi.server.util;

import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ResultFormatterSeparatedValues implements ResultFormatter {

    abstract protected void addCSVSeparator(StringBuilder resultBuilder, boolean hasNext);

    abstract protected String writeToCSVCellValue(JsonNode cell);

    abstract protected void writeAsCSVCell(StringBuilder resultBuilder, JsonNode node);


    @Override
    public String format(String s) throws ResultFormattingException {
        JsonNode jsonNode;
        try {
            jsonNode = RequestHelper.parse(s);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to parse result", e);
        }
        StringBuilder resultBuilder = new StringBuilder();
        writeArray(jsonNode, resultBuilder, "columns");
        JsonNode rows = jsonNode.get("data");
        if (rows.isArray()) {
            for (int j = 0; j < rows.size(); j++) {
                handleJsonRow(resultBuilder, rows, j);

            }
        }
        return resultBuilder.toString();
    }

    /*
         Additional complexity introduces to support cypher json results with arrays.

            json object like:
                [1,2,[3,4,5]]

            would translate to csv table format like:
                 1,2,3
                 1,2,4
                 1,2,5

         So result is de-normalized by repeating non-array columns as separate rows.
        At most one columns may contain an array.

        */
    private void handleJsonRow(StringBuilder resultBuilder, JsonNode rows, int j) {
        JsonNode row = rows.get(j);
        List<String> rowValues = new ArrayList<String>(rows.size());
        List<String> arrayCell = null;
        Integer arrayCellIndex = null;

        for (int i = 0; i < row.size(); i++) {
            String csvCellValue = "";
            JsonNode cell = row.get(i);
            if (cell.isArray()) {
                if (arrayCellIndex != null && arrayCellIndex != i) {
                    throw new IllegalArgumentException("csv conversion error: do not know how to convert json object with arrays in more than one column");
                } else {
                    arrayCellIndex = i;
                }
                for (JsonNode cellElem : cell) {
                    if (arrayCell == null) {
                        arrayCell = new ArrayList<String>();
                    }
                    arrayCell.add(writeToCSVCellValue(cellElem));
                }
            } else {
                csvCellValue = writeToCSVCellValue(cell);
            }

            rowValues.add(csvCellValue);
        }

        if (arrayCellIndex == null) {
            Iterator<String> iterator = rowValues.iterator();
            while (iterator.hasNext()) {
                resultBuilder.append(iterator.next());
                boolean hasNext = iterator.hasNext();
                addCSVSeparator(resultBuilder, hasNext);
            }
        } else {
            if (arrayCell != null) {
                for (String arrayElem : arrayCell) {
                    // write duplicate rows for each element in cell with array value
                    for (int i = 0; i < rowValues.size(); i++) {
                        if (i == arrayCellIndex) {
                            resultBuilder.append(arrayElem);
                        } else {
                            resultBuilder.append(rowValues.get(i));
                        }
                        addCSVSeparator(resultBuilder, i < rowValues.size() - 1);
                    }
                }
            }
        }
    }


    private void writeArray(JsonNode jsonNode, StringBuilder resultBuilder, String arrayName) {
        JsonNode array = jsonNode.get(arrayName);
        if (array.isArray()) {
            writeArray(resultBuilder, array);
        }
    }

    private void writeArray(StringBuilder resultBuilder, JsonNode array) {
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            appendValue(resultBuilder, iterator);
        }
    }

    private void appendValue(StringBuilder resultBuilder, Iterator<JsonNode> iterator) {
        JsonNode node = iterator.next();
        if (node.isArray()) {
            writeArray(resultBuilder, node);
        } else {
            writeObject(resultBuilder, node, iterator.hasNext());
        }
    }

    private void writeObject(StringBuilder resultBuilder, JsonNode node, boolean hasNext) {
        writeAsCSVCell(resultBuilder, node);
        addCSVSeparator(resultBuilder, hasNext);
    }


}
