package org.eol.globi.export;

import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.ExcelCSVPrinter;
import org.eol.globi.domain.Study;
import org.eol.globi.util.CSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportUnmatchedTaxonNames implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        String query = "START study = node:studies(title={study_title}) " +
                "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon " +
                "WITH distinct(taxon) as dtaxon, study " +
                "MATCH dtaxon-[sameAs?:SAME_AS]->otherTaxon " +
                "WHERE not(has(dtaxon.path)) AND otherTaxon = null " +
                "WITH dtaxon, otherTaxon, study " +
                "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->dtaxon, specimen-[:ORIGINALLY_DESCRIBED_AS]->origTaxon, dtaxon-[?:SIMILAR_TO]->ftaxon\n" +
                "RETURN distinct(origTaxon.name) as `unmatched taxon name(s)`" +
                ", dtaxon.statusLabel? as `name status`" +
                ", ftaxon.name? as `similar to taxon name`" +
                ", ftaxon.path? as `similar to taxon path`" +
                ", ftaxon.externalId? as `similar to taxon id`" +
                ", study.citation? as `study`" +
                ", study.source as `source`";

        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("study_title", study.getTitle());
        }};

        ExecutionResult rows = engine.execute(query, params);

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

}
