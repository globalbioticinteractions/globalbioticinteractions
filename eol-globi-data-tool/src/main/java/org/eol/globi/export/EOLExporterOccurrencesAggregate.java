package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class EOLExporterOccurrencesAggregate extends EOLExporterOccurrencesBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());
        ExecutionResult results = engine.execute(getQueryForDistinctTargetTaxaForPreyBySourceTaxa(study));
        Map<String, String> properties = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            populateRow(study, writer, properties, result);
        }
    }

    private void populateRow(Study study, Writer writer, Map<String, String> properties, Map<String, Object> result) throws IOException {
        Node predatorTaxon = (Node) result.get("predatorTaxon");
        JavaConversions.SeqWrapper<Node> preyTaxa = (JavaConversions.SeqWrapper<Node>) result.get("preyTaxa");
        Relationship relationship = (Relationship) result.get("interaction");

        for (Node preyTaxon : preyTaxa) {
            String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + predatorTaxon.getId() + "-" + relationship.getId() + "-" + preyTaxon.getId();
            writeRow(study, writer, properties, predatorTaxon, sourceOccurrenceId);
            String targetOccurrenceId = study.getUnderlyingNode().getId() + "-" + predatorTaxon.getId() + "-" + relationship.getId();
            writeRow(study, writer, properties, preyTaxon, targetOccurrenceId);
        }
    }

    private void writeRow(Study study, Writer writer, Map<String, String> properties, Node preyTaxon, String occurenceId) throws IOException {
        properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:" + occurenceId);
        properties.put(EOLDictionary.TAXON_ID, (String) preyTaxon.getProperty("externalId"));
        addProperty(properties, study.getUnderlyingNode(), Study.TITLE, EOLDictionary.EVENT_ID);
        writeProperties(writer, properties);
        properties.clear();
    }

}
