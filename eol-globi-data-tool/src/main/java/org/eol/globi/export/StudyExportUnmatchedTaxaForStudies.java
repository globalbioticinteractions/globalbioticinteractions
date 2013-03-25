package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.service.NoMatchService;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StudyExportUnmatchedTaxaForStudies implements StudyExporter {

    private GraphDatabaseService graphDbService;

    public StudyExportUnmatchedTaxaForStudies(GraphDatabaseService graphDatabaseService) {
        this.graphDbService = graphDatabaseService;
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String query = "START study = node:studies(\"*:*\") " +
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
}
