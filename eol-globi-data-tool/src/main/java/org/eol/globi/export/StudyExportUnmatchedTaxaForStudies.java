package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.service.NoMatchService;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StudyExportUnmatchedTaxaForStudies extends BaseExporter {

    public static final String META_TABLE_SUFFIX = "</location>\n" +
            "    </files>\n" +
            "    <field index=\"0\" term=\"http://rs.tdwg.org/dwc/terms/collectionID\"/>\n" +
            "    <field index=\"1\" term=\"http://rs.tdwg.org/dwc/terms/scientificName\"/>\n" +
            "  </table>\n";

    public static final String META_TABLE_PREFIX = "<table encoding=\"UTF-8\" fieldsTerminatedBy=\",\" linesTerminatedBy=\"\\n\" ignoreHeaderLines=\"1\" rowType=\"http://rs.tdwg.org/dwc/terms/text/DarwinRecord\">\n" +
            "    <files>\n" +
            "      <location>";
    private GraphDatabaseService graphDbService;

    public StudyExportUnmatchedTaxaForStudies(GraphDatabaseService graphDatabaseService) {
        this.graphDbService = graphDatabaseService;
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String query = "START study = node:studies(title=\"" + study.getTitle() + "\") " +
                "MATCH study-[:COLLECTED]->specimen-[:CLASSIFIED_AS]->taxon " +
                "WHERE taxon.externalId = \"" + NoMatchService.NO_MATCH + "\" " +
                "RETURN distinct taxon.name, study.title";

        ExecutionResult result = engine.execute(query);

        if (includeHeader) {
            writer.write("\"name of unmatched source taxon\"");
            writer.write(",\"study\"\n");
        }

        for (Map<String, Object> map : result) {
            writer.write("\"" + map.get("taxon.name") + "\",");
            writer.write("\"" + map.get("study.title") + "\"\n");
        }
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
