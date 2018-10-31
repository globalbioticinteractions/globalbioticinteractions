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
import java.util.stream.Stream;

public final class ExportUtil {

    interface ValueJoiner {
        String join(Stream<String> values);
    }

    static final class TSVValueJoiner implements ValueJoiner {

        @Override
        public String join(Stream<String> values) {
            return StringUtils.join(CSVTSVUtil.escapeValues(values), '\t');
        }
    }

    private static final TSVValueJoiner VALUE_JOINER_DEFAULT = new TSVValueJoiner();

    public static void writeResults(Writer writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader) throws IOException {
        writeResults(writer, dbService, query, params, includeHeader, VALUE_JOINER_DEFAULT);
    }

    public static void writeResults(Writer writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader, ValueJoiner joiner) throws IOException {
        ExecutionResult rows = new ExecutionEngine(dbService).execute(query, params);
        List<String> columns = rows.columns();
        if (includeHeader) {
            final String[] values = columns.toArray(new String[columns.size()]);
            writer.write(joiner.join(Stream.of(values)));
        }
        appendRow(writer, rows, columns, joiner);
    }

    public static void appendRow(Writer writer, Iterable<Map<String, Object>> rows, List<String> columns) throws IOException {
        appendRow(writer, rows, columns, VALUE_JOINER_DEFAULT);
    }

    public static void appendRow(Writer writer, Iterable<Map<String, Object>> rows, List<String> columns, ValueJoiner joiner) throws IOException {
        for (Map<String, Object> row : rows) {
            writer.write("\n");
            List<String> values = new ArrayList<String>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            writer.write(joiner.join(values.stream()));
        }
    }

    public static void writeProperties(Writer writer, Map<String, String> properties, String[] fields) throws IOException {
        writeProperties(writer, properties, fields, VALUE_JOINER_DEFAULT);
    }

    public static void writeProperties(Writer writer, Map<String, String> properties, String[] fields, ValueJoiner joiner) throws IOException {
        String values[] = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = properties.getOrDefault(fields[i], "");
        }
        writer.write(joiner.join(Stream.of(values)));
    }

    public static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }

}
