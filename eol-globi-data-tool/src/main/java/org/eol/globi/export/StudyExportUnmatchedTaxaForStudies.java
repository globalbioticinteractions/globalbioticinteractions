package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public abstract class StudyExportUnmatchedTaxaForStudies implements StudyExporter {

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        StringBuilder query = new StringBuilder();
        query.append("START study = node:studies(title=\"");
        query.append(study.getTitle());
        query.append("\") ");
        query.append(getQueryString(study));
        query.append(", taxon-[sameAs?:SAME_AS]->otherTaxon");
        query.append(" WHERE not(has(taxon.path)) AND sameAs IS NULL");
        query.append(" RETURN distinct description.name, description.externalId?, taxon.name, taxon.externalId?, study.title, study.source");

        ExecutionResult result = engine.execute(query.toString());

        if (includeHeader) {
            writeHeader(writer, getTaxonLabel());
        }

        for (Map<String, Object> map : result) {
            writeRow(writer, map);
        }
    }

    protected abstract String getQueryString(Study study);

    protected abstract String getTaxonLabel();

    protected void writeRow(Writer writer, Map<String, Object> map) throws IOException {
        writer.write("\"");
        writer.write((String) map.get("description.name"));
        writer.write("\",");
        Object externalId = map.get("description.externalId");
        writer.write((externalId == null ? "" : ("\"" + externalId + "\"")));
        writer.write(",");
        writer.write("\"");
        writer.write((String) map.get("taxon.name"));
        writer.write("\",");
        externalId = map.get("taxon.externalId");
        writer.write((externalId == null ? "" : ("\"" + externalId + "\"")));
        writer.write(",\"");
        writer.write((String) map.get("study.title"));
        writer.write("\",\"");
        writer.write((String) map.get("study.source"));
        writer.write("\"\n");

    }

    protected void writeHeader(Writer writer, String taxonLabel) throws IOException {
        writer.write("\"original " + taxonLabel + " taxon name\"");
        writer.write(",\"original " + taxonLabel + " external id\"");
        writer.write(",\"unmatched normalized " + taxonLabel + " taxon name\"");
        writer.write(",\"unmatched normalized " + taxonLabel + " external id\"");
        writer.write(",\"study\"");
        writer.write(",\"source\"\n");
    }

}
