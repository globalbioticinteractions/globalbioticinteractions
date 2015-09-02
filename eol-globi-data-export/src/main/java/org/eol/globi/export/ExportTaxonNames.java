package org.eol.globi.export;

import com.Ostermiller.util.CSVPrint;
import org.eol.globi.domain.Study;
import org.eol.globi.util.CSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportTaxonNames implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(Study study, Writer writer) {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        String query = "START taxon = node:taxons('*:*')\n" +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon\n" +
                "RETURN linkedTaxon.externalId? as id" +
                ", linkedTaxon.name? as name" +
                ", linkedTaxon.rank? as rank" +
                ", linkedTaxon.commonNames? as commonNames" +
                ", linkedTaxon.path? as path" +
                ", linkedTaxon.pathIds? as pathIds" +
                ", linkedTaxon.pathNames? as pathNames";

        HashMap<String, Object> params = new HashMap<String, Object>() {{
        }};

        ExecutionResult rows = engine.execute(query, params);
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
