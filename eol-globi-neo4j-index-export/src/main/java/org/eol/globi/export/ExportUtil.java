package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.CsvTranslators;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.CSVTSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public final class ExportUtil {

    public static void export(GraphDatabaseService graphService, String baseDir, String filename, String cypherQuery, ValueJoiner joiner) throws StudyImporterException {
        try {
            mkdirIfNeeded(baseDir);
            final FileOutputStream out = new FileOutputStream(new File(baseDir, filename));
            GZIPOutputStream os = new GZIPOutputStream(out);
            final Writer writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            Appender appender = AppenderWriter.of(writer, joiner);

            export(appender, graphService, cypherQuery);

            writer.flush();
            os.finish();
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(os);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export to [" + filename + "]", e);
        }
    }

    public static void export(Appender appender, GraphDatabaseService graphService, String query) throws IOException {
        HashMap<String, Object> params = new HashMap<String, Object>() {{
        }};
        writeResults(appender, graphService, query, params, true);
    }

    public interface ValueJoiner {
        String join(Stream<String> values);
    }

    interface Appender {
        void append(Stream<String> values) throws IOException;
    }

    public static final class AppenderWriter implements Appender {
        private final Writer writer;
        private final AtomicBoolean isFirstLine = new AtomicBoolean(true);
        private final ValueJoiner joiner;

        AppenderWriter(Writer writer) {
            this(writer, new TsvValueJoiner());
        }

        AppenderWriter(Writer writer, ValueJoiner joiner) {
            this.writer = writer;
            this.joiner = joiner;
        }

        @Override
        public void append(Stream<String> values) throws IOException {
            writer.write(joiner.join(values));
            writer.write("\n");
        }

        public static AppenderWriter of(Writer writer) {
            return new AppenderWriter(writer);
        }
        public static AppenderWriter of(Writer writer, ValueJoiner joiner) {
            return new AppenderWriter(writer, joiner);
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

    public static class NQuadValueJoiner implements ValueJoiner {
        @Override
        public String join(Stream<String> values) {
            String joined = values
                    .collect(Collectors.joining(" "));
            return StringUtils.isBlank(joined) ? "" : joined + " .";
        }
    }

    public static void writeResults(Appender appender, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader) throws IOException {
        ExecutionResult rows = new ExecutionEngine(dbService).execute(query, params);
        List<String> columns = rows.columns();
        if (includeHeader) {
            final String[] values = columns.toArray(new String[columns.size()]);
            appender.append(Stream.of(values));
        }
        appendRow(appender, rows, columns);
    }

    public static void appendRow(Appender appender, Iterable<Map<String, Object>> rows, List<String> columns) throws IOException {
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<String>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            appender.append(values.stream());
        }
    }

    public static void writeProperties(Appender appender, Map<String, String> properties, String[] fields) throws IOException {
        String values[] = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = properties.getOrDefault(fields[i], "");
        }
        appender.append(Stream.of(values));
    }

    public static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }

}
