package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public abstract class StudyExportUnmatchedTaxaForStudies extends BaseExporter {
    private static final String META_TABLE_SUFFIX = "</location>\n" +
            "    </files>\n" +
            "    <field index=\"0\" term=\"http://rs.tdwg.org/dwc/terms/scientificName\"/>\n" +
            "    <field index=\"1\" term=\"http://rs.tdwg.org/dwc/terms/scientificName\"/>\n" +
            "    <field index=\"2\" term=\"http://rs.tdwg.org/dwc/terms/collectionID\"/>\n" +
            "  </table>\n";
    private static final String META_TABLE_PREFIX = "<table encoding=\"UTF-8\" fieldsTerminatedBy=\",\" linesTerminatedBy=\"\\n\" ignoreHeaderLines=\"1\" rowType=\"http://rs.tdwg.org/dwc/terms/text/DarwinRecord\">\n" +
            "    <files>\n" +
            "      <location>";
    protected GraphDatabaseService graphDbService;

    public StudyExportUnmatchedTaxaForStudies(GraphDatabaseService graphDatabaseService) {
        this.graphDbService = graphDatabaseService;
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String query = getQueryString(study);

        ExecutionResult result = engine.execute(query);

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
        writer.write("\"" + map.get("taxon.name") + "\",");
        writer.write("\"" + map.get("study.title") + "\"\n");
    }

    protected void writeHeader(Writer writer, String taxonLabel) throws IOException {
        writer.write("\"original " + taxonLabel + " taxon name\"");
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
