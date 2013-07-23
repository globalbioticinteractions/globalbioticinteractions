package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterOccurrenceAggregates extends ExporterOccurrencesBase {

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
        Node predatorTaxon = (Node) result.get(QUERY_PARAM_SOURCE_TAXON);
        JavaConversions.SeqWrapper<Node> preyTaxa = (JavaConversions.SeqWrapper<Node>) result.get(QUERY_PARAM_TARGET_TAXA);
        String relationshipType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);

        for (Node preyTaxon : preyTaxa) {
            String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + predatorTaxon.getId() + "-" + relationshipType + "-" + preyTaxon.getId();
            writeRow(study, writer, properties, predatorTaxon, sourceOccurrenceId);
            String targetOccurrenceId = study.getUnderlyingNode().getId() + "-" + predatorTaxon.getId() + "-" + relationshipType;
            writeRow(study, writer, properties, preyTaxon, targetOccurrenceId);
        }
    }

    private void writeRow(Study study, Writer writer, Map<String, String> properties, Node taxon, String occurrenceId) throws IOException {
        properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:" + occurrenceId);
        properties.put(EOLDictionary.TAXON_ID, (String) taxon.getProperty("externalId"));
        addProperty(properties, study.getUnderlyingNode(), Study.TITLE, EOLDictionary.EVENT_ID);
        writeProperties(writer, properties);
        properties.clear();
    }

}
