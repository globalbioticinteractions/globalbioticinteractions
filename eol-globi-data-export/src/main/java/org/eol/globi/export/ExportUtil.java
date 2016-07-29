package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
            writer.write(StringUtils.join(values, '\t'));
        }

        for (Map<String, Object> row : rows) {
            writer.write("\n");
            List<String> values = new ArrayList<String>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            writer.write(StringUtils.join(values, '\t'));
        }
    }

    public static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }
}
