package org.eol.globi.export;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import scala.collection.convert.Wrappers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RollUpAssociations extends ExporterAssociationsBase {

    @Override
    public void doExportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());
        ExecutionResult results = executeQueryForDistinctTargetTaxaForPreyByStudy(engine, study.getTitle());

        final HTreeMap<String, Map<String, String>> assocIds = createMap();
        try {
            for (Map<String, Object> result : results) {
                putAssocIds(assocIds, result, study);
            }

            for (Map<String, String> entry : assocIds.values()) {
                writeProperties(writer, entry);
            }
        } finally {
            if (assocIds != null) {
                assocIds.close();
            }
        }
    }

    protected void putAssocIds(Map<String, Map<String, String>> assocIds, Map<String, Object> result, Study study) {
        TaxonNode sourceTaxon = new TaxonNode((Node) result.get(QUERY_PARAM_SOURCE_TAXON));
        List<Taxon> sTaxa = RollUpDistinctTaxa.expandTaxon(sourceTaxon);
        String relationshipType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);
        Wrappers.SeqWrapper<Node> targetTaxa = (Wrappers.SeqWrapper<Node>) result.get(QUERY_PARAM_TARGET_TAXA);
        for (Node targetTaxon : targetTaxa) {
            List<Taxon> tTaxa = RollUpDistinctTaxa.expandTaxon(new TaxonNode(targetTaxon));
            for (Taxon sTaxon : sTaxa) {
                for (Taxon tTaxon : tTaxa) {
                    Map<String, String> properties = new HashMap<String, String>();
                    String occSourceId1 = RollUpOccurrence.sourceOccurrenceId(study.getNodeID(), relationshipType, sTaxon);
                    String fullOccSourceId = RollUpOccurrence.fullSourceOccurrenceId(occSourceId1);
                    String fullOccTargetId = RollUpOccurrence.fullTargetOccurrenceId(occSourceId1, tTaxon);
                    String assocId = "globi:assoc:" + occSourceId1 + "-" + fillBlanks(tTaxon.getExternalId());

                    properties.put(EOLDictionary.ASSOCIATION_ID, assocId);
                    properties.put(EOLDictionary.OCCURRENCE_ID, fullOccSourceId);
                    properties.put(EOLDictionary.TARGET_OCCURRENCE_ID, fullOccTargetId);
                    properties.put(EOLDictionary.ASSOCIATION_TYPE, getEOLTermFor(relationshipType));
                    addStudyInfo(study, properties);
                    assocIds.put(assocId, properties);
                }
            }
        }
    }

    public static String fillBlanks(String externalId) {
        return StringUtils.replace(externalId, " ", "_");
    }

    protected HTreeMap<String, Map<String, String>> createMap() {
        return DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make()
                .createHashMap("assocIds")
                .make();
    }

}
