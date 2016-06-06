package org.eol.globi.export;

import com.Ostermiller.util.CSVPrint;
import org.apache.commons.io.FileUtils;
import org.eol.globi.util.CSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExportUtil {

    public static void writeResults(Writer writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params, boolean includeHeader) {
        ExecutionResult rows = new ExecutionEngine(dbService).execute(query, params);
        CSVPrint printer = CSVUtil.createCSVPrint(writer);
        List<String> columns = rows.columns();
        if (includeHeader) {
            printer.print(columns.toArray(new String[columns.size()]));
        }

        for (Map<String, Object> row : rows) {
            printer.println();
            for (String column : columns) {
                Object value = row.get(column);
                printer.print(value == null ? "" : value.toString());
            }
        }
    }

    public static void mkdirIfNeeded(String baseDir) throws IOException {
        final File parentDir = new File(baseDir);
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }
}
