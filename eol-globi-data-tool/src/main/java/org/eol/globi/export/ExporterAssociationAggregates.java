package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import scala.collection.JavaConversions;
import scala.collection.convert.Wrappers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterAssociationAggregates extends ExporterAssociationsBase {

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
        Node sourceTaxon = (Node) result.get(QUERY_PARAM_SOURCE_TAXON);
        Wrappers.SeqWrapper<Node> targetTaxa = (Wrappers.SeqWrapper) result.get(QUERY_PARAM_TARGET_TAXA);
        String interactionType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);

        for (Node preyTaxon : targetTaxa) {
            String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + sourceTaxon.getId() + "-" + interactionType;
            String assocIdAndTargetOccurrenceIdId = sourceOccurrenceId + "-" + preyTaxon.getId();
            properties.put(EOLDictionary.ASSOCIATION_ID, "globi:assoc:" + assocIdAndTargetOccurrenceIdId);
            properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:source:" + sourceOccurrenceId);
            properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, "globi:occur:target:" + assocIdAndTargetOccurrenceIdId);
            properties.put(EOLDictionary.ASSOCIATION_TYPE, getEOLTermFor(interactionType));
            addStudyInfo(study, properties);
            writeProperties(writer, properties);
            properties.clear();
        }
    }

}
