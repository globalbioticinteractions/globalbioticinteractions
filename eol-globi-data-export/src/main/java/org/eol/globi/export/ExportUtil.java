package org.eol.globi.export;

import com.Ostermiller.util.CSVPrint;
import org.eol.globi.util.CSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExportUtil {

    public static void writeResults(Writer writer, GraphDatabaseService dbService, String query, HashMap<String, Object> params) {
        ExecutionResult rows = new ExecutionEngine(dbService).execute(query, params);
        CSVPrint printer = CSVUtil.createCSVPrint(writer);
        List<String> columns = rows.columns();
        printer.print(columns.toArray(new String[columns.size()]));

        for (Map<String, Object> row : rows) {
            printer.println();
            for (String column : columns) {
                Object value = row.get(column);
                printer.print(value == null ? "" : value.toString());
            }
        }
    }
}
