package org.eol.globi.export;

import com.Ostermiller.util.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.neo4j.cypher.CypherParser;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StudyExportUnmatchedTaxaForStudies implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        StringBuilder query = new StringBuilder();
        query.append("START study = node:studies(title={study_title}) ");
        query.append(getQueryString(study));
        query.append(", taxon-[sameAs?:SAME_AS]->otherTaxon");
        query.append(" WHERE not(has(taxon.path)) AND sameAs IS NULL");
        query.append(" RETURN distinct description.name, description.externalId?, taxon.name, taxon.externalId?, study.title, study.source");

        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("study_title", study.getTitle());
        }};

        ExecutionResult result = engine.execute(query.toString(), params);
        if (includeHeader) {
            writeHeader(writer, getTaxonLabel());
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

    protected abstract String getQueryString(Study study);

    protected abstract String getTaxonLabel();

    protected void writeHeader(Writer writer, String taxonLabel) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer);
        printer.println(new String[]{"original " + taxonLabel + " taxon name"
                , "original " + taxonLabel + " external id"
                , "unmatched normalized " + taxonLabel + " taxon name"
                , "unmatched normalized " + taxonLabel + " external id"
                , "study"
                , "source"});
    }

}
