package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.CsvTranslators;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.CSVTSVUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public final class ExportUtil {

    public static void export(GraphDatabaseService graphService, String filename, String cypherQuery, ValueJoiner joiner) throws StudyImporterException {
        export(graphService, filename, Collections.singletonList(cypherQuery), joiner);
    }

    public static void export(GraphDatabaseService graphService, String filename, List<String> cypherQueries, ValueJoiner joiner) throws StudyImporterException {
        try {
            File file = new File(filename);
            mkdirIfNeeded(file.getParent());
            final FileOutputStream out = new FileOutputStream(file);
            GZIPOutputStream os = new GZIPOutputStream(out);
            final Writer writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            Appender appender = AppenderWriter.of(writer, joiner);

            export(appender, graphService, cypherQueries);

            writer.flush();
            os.finish();
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(os);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export to [" + filename + "]", e);
        }
    }

    public static void export(Appender appender, GraphDatabaseService graphService, String query) throws IOException {
        export(appender, graphService, Collections.singletonList(query));
    }

    public static void export(Appender appender, GraphDatabaseService graphService, List<String> queries) throws IOException {
        Map<String, Object> params = new TreeMap<>();
        writeResults(appender, graphService, queries, params, true);
    }

    public interface ValueJoiner {
        String join(Stream<String> values);
    }

    interface Appender {
        void append(Stream<String> values) throws IOException;
    }

    public static final class AppenderWriter implements Appender {
        private final Writer writer;
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

    static void writeResults(Appender appender, GraphDatabaseService dbService, String query, Map<String, Object> params, boolean includeHeader) throws IOException {
        writeResults(appender, dbService, Collections.singletonList(query), params, includeHeader);
    }

    private static void writeResults(Appender appender, GraphDatabaseService dbService, List<String> queries, Map<String, Object> params, boolean includeHeader) throws IOException {
        for (String query : queries) {
            Result rows = dbService.execute(query, params);
            List<String> columns = rows.columns();
            if (includeHeader && queries.indexOf(query) == 0) {
                final String[] values = columns.toArray(new String[0]);
                appender.append(Stream.of(values));
            }
            appendRow(appender, rows, columns);
        }
    }

    static void appendRow(Appender appender, Iterator<Map<String, Object>> rows, List<String> columns) throws IOException {
        Map<String, Object> row;
        while (rows.hasNext()) {
            row = rows.next();
            List<String> values = new ArrayList<String>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            appender.append(values.stream());
        }
    }

    public static void writeProperties(Appender appender, Map<String, String> properties, String[] fields) throws IOException {
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = properties.getOrDefault(fields[i], "");
        }
        appender.append(Stream.of(values));
    }

    static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }

}
