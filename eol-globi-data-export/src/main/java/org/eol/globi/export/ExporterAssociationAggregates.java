package org.eol.globi.export;

import org.eol.globi.domain.Study;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import scala.collection.convert.Wrappers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterAssociationAggregates extends ExporterAssociationsBase {

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());
        ExecutionResult results = executeQueryForDistinctTargetTaxaForPreyByStudy(engine, study.getTitle());
        Map<String, String> properties = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            populateRow(study, writer, properties, result);
        }
    }

    private void populateRow(Study study, Writer writer, Map<String, String> properties, Map<String, Object> result) throws IOException {
        Long sourceTaxonId = (Long) result.get(QUERY_PARAM_SOURCE_TAXON_ID);
        Wrappers.SeqWrapper<Long> targetTaxonIds = (Wrappers.SeqWrapper) result.get(QUERY_PARAM_TARGET_TAXON_IDS);
        String interactionType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);

        for (Long targetTaxonId : targetTaxonIds) {
            String sourceOccurrenceId = referenceId(study) + "-" + sourceTaxonId + "-" + interactionType;
            String assocIdAndTargetOccurrenceIdId = sourceOccurrenceId + "-" + targetTaxonId;
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
