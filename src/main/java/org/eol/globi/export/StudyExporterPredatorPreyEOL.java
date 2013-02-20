package org.eol.globi.export;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.eol.globi.domain.Study;

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
                "predator-[rel:ATE|PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH]->prey-[:CLASSIFIED_AS]->preyTaxon " +
                 "," +
                 "predator-[:CLASSIFIED_AS]-predatorTaxon " +
                "WHERE has(predatorTaxon.externalId) AND has(preyTaxon.externalId) " +
                "RETURN distinct predatorTaxon.externalId, type(rel) as relType, preyTaxon.externalId";

        ExecutionResult result = engine.execute(query);
        for (Map<String, Object> map : result) {
            writer.write("\"" + map.get("predatorTaxon.externalId") + "\",");
            writer.write("\"" + map.get("relType") + "\",");
            writer.write("\"" + map.get("preyTaxon.externalId") + "\"\n");

        }
    }
}
