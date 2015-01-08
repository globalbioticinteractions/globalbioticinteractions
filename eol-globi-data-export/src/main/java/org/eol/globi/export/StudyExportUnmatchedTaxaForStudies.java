package org.eol.globi.export;

import com.Ostermiller.util.CSVPrinter;
import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyExportUnmatchedTaxaForStudies implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        StringBuilder query = new StringBuilder();
        query.append("START study = node:studies(title={study_title}) ");
        query.append(getQueryString());
        query.append(", taxon-[sameAs?:SAME_AS]->otherTaxon");
        query.append(" WHERE not(has(taxon.path)) AND sameAs IS NULL");
        query.append(" RETURN distinct description.name, description.externalId?, taxon.name, taxon.externalId?, study.title, study.source");

        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("study_title", study.getTitle());
        }};

        ExecutionResult result = engine.execute(query.toString(), params);
        if (includeHeader) {
            writeHeader(writer);
        }

        CSVPrinter printer = new CSVPrinter(writer);
        List<String> columns = result.columns();
        for (Map<String, Object> map : result) {
            for (String column : columns) {
                Object value = map.get(column);
                printer.print(value == null ? "" : value.toString());
            }
            printer.println();
        }
    }

    private String getQueryString() {
        return "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon, " +
                        "specimen-[:ORIGINALLY_DESCRIBED_AS]->description ";
    }

    protected void writeHeader(Writer writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer);
        printer.println(new String[]{"original taxon name"
                , "original taxon external id"
                , "unmatched normalized taxon name"
                , "unmatched normalized taxon external id"
                , "study"
                , "source"});
    }

}
