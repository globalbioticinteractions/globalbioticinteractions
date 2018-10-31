package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.CsvTranslators;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ExportUtil {

    interface ValueJoiner {
        String join(Stream<String> values);
    }

    interface Appender {
        void append(String value) throws IOException;
    }

    public static final class AppenderWriter implements Appender {

        private final Writer writer;

        AppenderWriter(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void append(String value) throws IOException {
            writer.write(value);
        }

        public static AppenderWriter of(Writer writer) {
            return new AppenderWriter(writer);
        }
    }

    static final class TsvValueJoiner implements ValueJoiner {

        @Override
        public String join(Stream<String> values) {
            return StringUtils.join(CSVTSVUtil.escapeValues(values), '\t');
        }
    }

    public static class CsvValueJoiner implements ValueJoiner {

        private final CsvTranslators.CsvEscaper escaper = new CsvTranslators.CsvEscaper();

        @Override
        public String join(Stream<String> values) {
            return values
                    .map(escaper::translate)
                    .collect(Collectors.joining(","));
        }
    }

    private static final ValueJoiner VALUE_JOINER_DEFAULT = new TsvValueJoiner();

    public static void writeResults(Appender writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader) throws IOException {
        writeResults(writer, dbService, query, params, includeHeader, VALUE_JOINER_DEFAULT);
    }

    public static void writeResults(Appender writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader, ValueJoiner joiner) throws IOException {
        ExecutionResult rows = new ExecutionEngine(dbService).execute(query, params);
        List<String> columns = rows.columns();
        if (includeHeader) {
            final String[] values = columns.toArray(new String[columns.size()]);
            writer.append(joiner.join(Stream.of(values)));
        }
        appendRow(writer, rows, columns, joiner);
    }

    public static void appendRow(Appender writer, Iterable<Map<String, Object>> rows, List<String> columns) throws IOException {
        appendRow(writer, rows, columns, VALUE_JOINER_DEFAULT);
    }

    public static void appendRow(Appender writer, Iterable<Map<String, Object>> rows, List<String> columns, ValueJoiner joiner) throws IOException {
        for (Map<String, Object> row : rows) {
            writer.append("\n");
            List<String> values = new ArrayList<String>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            writer.append(joiner.join(values.stream()));
        }
    }

    public static void writeProperties(Appender appender, Map<String, String> properties, String[] fields) throws IOException {
        writeProperties(appender, properties, fields, VALUE_JOINER_DEFAULT);
    }

    public static void writeProperties(Appender appender, Map<String, String> properties, String[] fields, ValueJoiner joiner) throws IOException {
        String values[] = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = properties.getOrDefault(fields[i], "");
        }
        appender.append(joiner.join(Stream.of(values)));
    }

    public static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }

}
