package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CSVTSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExportUtil {

    public static void writeResults(Writer writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader) throws IOException {
        ExecutionResult rows = new ExecutionEngine(dbService).execute(query, params);
        List<String> columns = rows.columns();
        if (includeHeader) {
            final String[] values = columns.toArray(new String[columns.size()]);
            writer.write(StringUtils.join(CSVTSVUtil.escapeValues(values), '\t'));
        }

        appendRow(writer, rows, columns);
    }

    public static void appendRow(Writer writer, Iterable<Map<String, Object>> rows, List<String> columns) throws IOException {
        for (Map<String, Object> row : rows) {
            writer.write("\n");
            List<String> values = new ArrayList<String>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            writer.write(StringUtils.join(CSVTSVUtil.escapeValues(values.stream()), '\t'));
        }
    }

    public static void writeProperties(Writer writer, Map<String, String> properties, String[] fields) throws IOException {
        String values[] = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            if (properties.containsKey(fields[i])) {
                values[i] = properties.get(fields[i]);
            } else {
                values[i] = "";
            }
        }
        writer.write(StringUtils.join(CSVTSVUtil.escapeValues(values), '\t'));
    }

    public static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }

}
