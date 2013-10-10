package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CypherQueryExecutor {
    private static final Log LOG = LogFactory.getLog(CypherQueryExecutor.class);
    private final String query;
    private final Map<String, String> params;

    public CypherQueryExecutor(String query, Map<String, String> queryParams) {
        this.query = query;
        this.params = queryParams;
    }

    public String execute(HttpServletRequest request) throws IOException {
        LOG.info("executing query: [" + query + "] with params [" + params + "]");
        String result;
        String type = request == null ? "json" : request.getParameter("type");
        if (type == null || "json".equalsIgnoreCase(type)) {
            result = executeRemote();
        } else if ("csv".equalsIgnoreCase(type)) {
            result = executeAndTransformToCSV();
        } else if ("json.v2".equalsIgnoreCase(type)) {
            result = executeAndTransformToJSONv2();
        } else {
            throw new IOException("found unsupported return format type request for [" + type + "]");
        }
        return result;
    }

    public String executeAndTransformToJSONv2() throws IOException {
        JsonNode jsonNode = execute();
        return CypherResultFormatter.format(jsonNode);
    }

    private String executeRemote() throws IOException {
        org.apache.http.client.HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://46.4.36.142:7474/db/data/cypher");
        HttpClient.addJsonHeaders(httpPost);
        httpPost.setEntity(new StringEntity(wrapQuery(query, params)));
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return httpClient.execute(httpPost, responseHandler);
    }

    private String executeAndTransformToCSV() throws IOException {
        JsonNode jsonNode = execute();
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

    private void addCSVSeparator(StringBuilder resultBuilder, boolean hasNext) {
        resultBuilder.append(hasNext ? "," : "\n");
    }

    private boolean hasArrayCells(List<String> arrayCell) {
        return arrayCell == null;
    }

    private String writeToCSVCellValue(JsonNode cell) {
        StringBuilder builder = new StringBuilder();
        writeAsCSVCell(builder, cell);
        return builder.toString();
    }

    private JsonNode execute() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(executeRemote());
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

    private void writeAsCSVCell(StringBuilder resultBuilder, JsonNode node) {
        if (!node.isNull()) {
            if (node.isTextual()) {
                resultBuilder.append("\"");
            }
            resultBuilder.append(node.getValueAsText());
            if (node.isTextual()) {
                resultBuilder.append("\"");
            }
        }
    }

    private String wrapQuery(String cypherQuery, Map<String, String> params) {
        String query = CypherProxyController.JSON_CYPHER_WRAPPER_PREFIX;
        query += cypherQuery;
        query += " \", \"params\": {" + buildJSONParamList(params) + " } }";
        return query;
    }

    private String buildJSONParamList(Map<String, String> paramMap) {
        StringBuilder builder = new StringBuilder();
        if (paramMap != null) {
            populateParams(paramMap, builder);
        }
        return builder.toString();
    }

    private void populateParams(Map<String, String> paramMap, StringBuilder builder) {
        Iterator<Map.Entry<String, String>> iterator = paramMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            String jsonParam = "\"" + param.getKey() + "\" : \"" + param.getValue() + "\"";
            builder.append(jsonParam);
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
    }

}
