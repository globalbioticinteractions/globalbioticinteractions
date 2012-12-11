package org.trophic.graph.export;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.domain.Study;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class StudyExporterPredatorPreyEOL implements StudyExporter {

    private GraphDatabaseService graphDbService;

    public StudyExporterPredatorPreyEOL(GraphDatabaseService graphDatabaseService) {
        this.graphDbService = graphDatabaseService;
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String query = "START study = node:studies('*:*') " +
                "MATCH " +
                "study-[:COLLECTED]->predator, " +
                "predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon " +
                 "," +
                 "predator-[:CLASSIFIED_AS]-predatorTaxon " +
                "WHERE has(predatorTaxon.externalId) AND has(preyTaxon.externalId) " +
                "RETURN distinct predatorTaxon.externalId, preyTaxon.externalId";

        ExecutionResult result = engine.execute(query);
        for (Map<String, Object> map : result) {
            writer.write("\"" + map.get("predatorTaxon.externalId") + "\",");
            writer.write("\"" + map.get("preyTaxon.externalId") + "\",");
            writer.write("\"feeds on\"\n");
        }
    }
}
