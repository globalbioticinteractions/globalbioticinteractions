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

public class EOLExporterAssociationAggregates extends EOLExporterAssociationsBase {

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
            String assocId = study.getUnderlyingNode().getId() + "-" + predatorTaxon.getId() + "-" + relationshipType + "-" + preyTaxon.getId();
            properties.put(EOLDictionary.ASSOCIATION_ID, "globi:assoc:" + assocId);
            String sourceOccurrenceId = study.getUnderlyingNode().getId() + "-" + predatorTaxon.getId() + "-" + relationshipType;
            properties.put(EOLDictionary.OCCURRENCE_ID, "globi:occur:source:" + sourceOccurrenceId);
            properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, "globi:occur:target:" + assocId);
            properties.put(EOLDictionary.ASSOCIATION_TYPE, relationshipType);
            properties.put(EOLDictionary.SOURCE, study.getTitle());
            writeProperties(writer, properties);
            properties.clear();
        }
    }

}
