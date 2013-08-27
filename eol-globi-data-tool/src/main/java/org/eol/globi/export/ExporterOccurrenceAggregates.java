package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import scala.collection.JavaConversions;
import scala.collection.convert.Wrappers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterOccurrenceAggregates extends ExporterOccurrencesBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());
        ExecutionResult results = engine.execute(getQueryForDistinctTargetTaxaForPreyBySourceTaxa(study));

        HashMap<String, String> properties = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            populateRow(study, writer, result, properties);
        }
    }

    private void populateRow(Study study, Writer writer, Map<String, Object> result, Map<String, String> properties) throws IOException {
        Taxon sourceTaxon = new Taxon((Node) result.get(QUERY_PARAM_SOURCE_TAXON));
        String relationshipType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);

        String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + sourceTaxon.getNodeID() + "-" + relationshipType;
        writeRow(study, writer, properties, sourceTaxon, "globi:occur:source:" + sourceOccurrenceId);

        Wrappers.SeqWrapper<Node> targetTaxa = (Wrappers.SeqWrapper<Node>) result.get(QUERY_PARAM_TARGET_TAXA);
        for (Node targetTaxon : targetTaxa) {
            Taxon taxon = new Taxon(targetTaxon);
            String targetOccurrenceId = sourceOccurrenceId + "-" + taxon.getNodeID();
            writeRow(study, writer, properties, taxon, "globi:occur:target:" + targetOccurrenceId);
        }
    }

    private void writeRow(Study study, Writer writer, Map<String, String> properties, Taxon taxon, String idPrefix) throws IOException {
        properties.put(EOLDictionary.OCCURRENCE_ID, idPrefix);
        properties.put(EOLDictionary.TAXON_ID, taxon.getExternalId());
        addProperty(properties, study.getUnderlyingNode(), Study.TITLE, EOLDictionary.EVENT_ID);
        writeProperties(writer, properties);
        properties.clear();
    }

}
