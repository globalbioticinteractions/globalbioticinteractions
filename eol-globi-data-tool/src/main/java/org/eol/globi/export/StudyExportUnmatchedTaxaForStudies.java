package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public abstract class StudyExportUnmatchedTaxaForStudies extends DarwinCoreExporter {
    private static final String META_TABLE_SUFFIX = "</location>\n" +
            "    </files>\n" +
            "    <field index=\"0\" term=\"" + EOLDictionary.SCIENTIFIC_NAME + "\"/>\n" +
            "    <field index=\"1\" term=\"" + EOLDictionary.TAXON_ID + "\"/>\n" +
            "    <field index=\"2\" term=\"" + EOLDictionary.SCIENTIFIC_NAME + "\"/>\n" +
            "    <field index=\"3\" term=\"" + EOLDictionary.COLLECTION_CODE + "\"/>\n" +
            "  </table>\n";
    private static final String META_TABLE_PREFIX = "<table encoding=\"UTF-8\" fieldsTerminatedBy=\",\" linesTerminatedBy=\"\\n\" ignoreHeaderLines=\"1\" rowType=\"http://rs.tdwg.org/dwc/terms/text/DarwinRecord\">\n" +
            "    <files>\n" +
            "      <location>";

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());

        StringBuilder query = new StringBuilder();
        query.append("START study = node:studies(title=\"");
        query.append(study.getTitle());
        query.append("\") ");
        query.append(getQueryString(study));
        query.append("WHERE not(has(taxon.path))");
        query.append(" RETURN distinct description.name, taxon.externalId?, taxon.name, study.title");

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
        writer.write("\"" + map.get("description.name") + "\",");
        Object externalId = map.get("taxon.externalId");
        writer.write((externalId == null ? "" : ("\"" + externalId + "\"")));
        writer.write(",");
        writer.write("\"" + map.get("taxon.name") + "\",");
        writer.write("\"" + map.get("study.title") + "\"\n");
    }

    protected void writeHeader(Writer writer, String taxonLabel) throws IOException {
        writer.write("\"original " + taxonLabel + " taxon name\"");
        writer.write(",\"unmatched normalized " + taxonLabel + " external id\"");
        writer.write(",\"unmatched normalized " + taxonLabel + " taxon name\"");
        writer.write(",\"study\"\n");
    }


    @Override
    protected String getMetaTablePrefix() {
        return META_TABLE_PREFIX;
    }

    @Override
    protected String getMetaTableSuffix() {
        return META_TABLE_SUFFIX;
    }
}
