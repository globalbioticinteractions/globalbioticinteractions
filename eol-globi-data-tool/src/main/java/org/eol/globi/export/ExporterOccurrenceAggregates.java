package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import scala.collection.convert.Wrappers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterOccurrenceAggregates extends ExporterOccurrencesBase {

    @Override
    public void doExportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());
        ExecutionResult results = executeQueryForDistinctTargetTaxaForPreyByStudy(engine, study.getTitle());

        HashMap<String, String> properties = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            populateRow(study, writer, result, properties);
        }
    }

    private void populateRow(Study study, Writer writer, Map<String, Object> result, Map<String, String> properties) throws IOException {
        TaxonNode sourceTaxon = new TaxonNode((Node) result.get(QUERY_PARAM_SOURCE_TAXON));
        String relationshipType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);

        String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + sourceTaxon.getNodeID() + "-" + relationshipType;
        writeRow(writer, properties, sourceTaxon, "globi:occur:source:" + sourceOccurrenceId);

        Wrappers.SeqWrapper<Node> targetTaxa = (Wrappers.SeqWrapper<Node>) result.get(QUERY_PARAM_TARGET_TAXA);
        for (Node targetTaxon : targetTaxa) {
            TaxonNode taxon = new TaxonNode(targetTaxon);
            String targetOccurrenceId = sourceOccurrenceId + "-" + taxon.getNodeID();
            writeRow(writer, properties, taxon, "globi:occur:target:" + targetOccurrenceId);
        }
    }

    private void writeRow(Writer writer, Map<String, String> properties, TaxonNode taxon, String idPrefix) throws IOException {
        properties.put(EOLDictionary.OCCURRENCE_ID, idPrefix);
        properties.put(EOLDictionary.TAXON_ID, taxon.getExternalId());
        writeProperties(writer, properties);
        properties.clear();
    }

}
