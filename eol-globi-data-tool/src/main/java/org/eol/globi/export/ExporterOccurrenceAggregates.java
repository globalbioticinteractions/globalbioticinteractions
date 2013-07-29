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

        for (Map<String, Object> result : results) {
            populateRow(study, writer, result);
        }
    }

    private void populateRow(Study study, Writer writer, Map<String, Object> result) throws IOException {
        Map<String, String> sourceProperties = new HashMap<String, String>();
        Node sourceTaxon = (Node) result.get(QUERY_PARAM_SOURCE_TAXON);
        String relationshipType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);

        String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + sourceTaxon.getId() + "-" + relationshipType;
        writeRow(study, writer, sourceProperties, sourceTaxon, "globi:occur:source:" + sourceOccurrenceId);

        JavaConversions.SeqWrapper<Node> targetTaxa = (JavaConversions.SeqWrapper<Node>) result.get(QUERY_PARAM_TARGET_TAXA);
        for (Node targetTaxon : targetTaxa) {
            Map<String, String> targetProperties = new HashMap<String, String>();
            String targetOccurrenceId = sourceOccurrenceId + "-" + targetTaxon.getId();
            writeRow(study, writer, targetProperties, targetTaxon, "globi:occur:target:" + targetOccurrenceId);
        }
    }

    private void writeRow(Study study, Writer writer, Map<String, String> properties, Node taxon, String idPrefix) throws IOException {
        properties.put(EOLDictionary.OCCURRENCE_ID, idPrefix);
        properties.put(EOLDictionary.TAXON_ID, (String) taxon.getProperty("externalId"));
        addProperty(properties, study.getUnderlyingNode(), Study.TITLE, EOLDictionary.EVENT_ID);
        writeProperties(writer, properties);
        properties.clear();
    }

}
