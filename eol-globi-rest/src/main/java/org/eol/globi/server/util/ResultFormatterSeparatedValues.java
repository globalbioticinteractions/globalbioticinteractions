package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.VALUE_FALSE;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_FLOAT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_STRING;
import static com.fasterxml.jackson.core.JsonToken.VALUE_TRUE;


public abstract class ResultFormatterSeparatedValues extends ResultFormatterStreamingImpl {

    abstract protected void addCSVSeparator(StringBuilder resultBuilder, boolean hasNext);

    abstract protected String getFieldSeparator();

    abstract protected String getStringQuotes();

    abstract protected String escapeValue(String value);

    abstract protected String writeToCSVCellValue(JsonNode cell);

    abstract protected void writeAsCSVCell(StringBuilder resultBuilder, JsonNode node);

    @Override
    protected void handleRows(OutputStream os, JsonParser jsonParser) throws IOException {
        JsonToken token;
        token = jsonParser.nextToken();
        if (START_ARRAY.equals(token)) {
            boolean isFirstValue = true;
            boolean inValueArray = false;
            boolean inRowArray = false;
            String currentFieldName = null;
            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            while ((token = jsonParser.nextToken()) != null) {
                if (FIELD_NAME.equals(token)) {
                    currentFieldName = jsonParser.getCurrentName();
                }
                if (START_ARRAY.equals(token)) {
                    if (inRowArray && !StringUtils.equals("meta", currentFieldName)) {
                        inValueArray = true;
                    } else {
                        isFirstValue = true;
                        inRowArray = true;
                    }
                } else if (isValue(token)) {
                    if (!isFirstValue && !inValueArray) {
                        if (!StringUtils.equals("meta", currentFieldName)) {
                            IOUtils.write(getFieldSeparator(), lineBuffer, StandardCharsets.UTF_8);
                        }
                    }
                    if (inValueArray) {
                        if (lineBuffer.size() > 0) {
                            lineBuffer.writeTo(os);
                            IOUtils.write(getFieldSeparator(), os, StandardCharsets.UTF_8);
                            writeValue(os, jsonParser, token);
                            addNewline(os);
                        }
                    } else {
                        writeValue(lineBuffer, jsonParser, token);
                    }
                    isFirstValue = false;
                } else if (END_ARRAY.equals(token)) {
                    if (inValueArray) {
                        inValueArray = false;
                    } else if (inRowArray) {
                        if (lineBuffer.size() > 0) {
                            addNewline(lineBuffer);
                            lineBuffer.writeTo(os);
                        }
                        inRowArray = false;
                    }
                    lineBuffer = new ByteArrayOutputStream();


                }
            }
        }
    }

    @Override
    protected void handleHeader(OutputStream os, JsonParser jsonParser) throws IOException {
        JsonToken token;
        token = jsonParser.nextToken();
        if (START_ARRAY.equals(token)) {
            boolean isFirstValue = true;
            while ((token = jsonParser.nextToken()) != null && !END_ARRAY.equals(token)) {
                if (isValue(token)) {
                    if (!isFirstValue) {
                        IOUtils.write(getFieldSeparator(), os, StandardCharsets.UTF_8);
                    }
                    writeValue(os, jsonParser, token);

                    isFirstValue = false;
                }
            }
            addNewline(os);
        }
    }

    private void addNewline(OutputStream os) throws IOException {
        IOUtils.write("\n", os, StandardCharsets.UTF_8);
        os.flush();
    }

    public void writeValue(OutputStream os, JsonParser jsonParser, JsonToken token) throws IOException {
        if (VALUE_STRING.equals(token)) {
            IOUtils.write(getStringQuotes(), os, StandardCharsets.UTF_8);
        }

        String text = VALUE_NULL.equals(token) ? "" : jsonParser.getText();
        IOUtils.write(escapeValue(text), os, StandardCharsets.UTF_8);

        if (VALUE_STRING.equals(token)) {
            IOUtils.write(getStringQuotes(), os, StandardCharsets.UTF_8);
        }
    }

    public static boolean isValue(JsonToken token) {
        return VALUE_STRING.equals(token)
                || VALUE_FALSE.equals(token)
                || VALUE_NUMBER_FLOAT.equals(token)
                || VALUE_TRUE.equals(token)
                || VALUE_NUMBER_INT.equals(token)
                || VALUE_NULL.equals(token);
    }

    @Override
    public String format(String s) throws ResultFormattingException {
        JsonNode jsonNode;
        try {
            jsonNode = RequestHelper.parse(s);
        } catch (IOException e) {
            throw new ResultFormattingException("failed to parse result", e);
        }

        try {
            RequestHelper.throwOnError(jsonNode);
        } catch (IOException e) {
            throw new ResultFormattingException("result has errors", e);
        }

        StringBuilder resultBuilder = new StringBuilder();
        writeArray(jsonNode, resultBuilder);
        JsonNode rowsAndMetas = jsonNode.get("data");
        if (rowsAndMetas.isArray()) {
            for (int j = 0; j < rowsAndMetas.size(); j++) {
                handleJsonRow(resultBuilder, rowsAndMetas, j);

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
    private void handleJsonRow(StringBuilder resultBuilder, JsonNode rowsAndMetas, int j) {
        JsonNode row = RequestHelper.getRow(rowsAndMetas.get(j));
        List<String> rowValues = new ArrayList<>(rowsAndMetas.size());
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
                        arrayCell = new ArrayList<>();
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


    private void writeArray(JsonNode jsonNode, StringBuilder resultBuilder) {
        JsonNode array = jsonNode.get("columns");
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
