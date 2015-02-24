package org.eol.globi.export;

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

public class RollUpOccurrence extends ExporterOccurrencesBase {

    public static final String GLOBI_OCCUR_RSOURCE = "globi:occur:rsource:";
    public static final String GLOBI_OCCUR_RTARGET = "globi:occur:rtarget:";

    @Override
    public void doExportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase());
        ExecutionResult results = executeQueryForDistinctTargetTaxaForPreyByStudy(engine, study.getTitle());

        final HTreeMap<String, String> occIds = createMap();
        try {
            for (Map<String, Object> result : results) {
                putOccIds(occIds, result, study);
            }

            Map<String, String> properties = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : occIds.entrySet()) {
                properties.put(EOLDictionary.OCCURRENCE_ID, entry.getKey());
                properties.put(EOLDictionary.TAXON_ID, entry.getValue());
                writeProperties(writer, properties);
                properties.clear();
            }
        } finally {
            if (occIds != null) {
                occIds.close();
            }
        }
    }

    protected void putOccIds(Map<String, String> occIds, Map<String, Object> result, Study study) {
        TaxonNode sourceTaxon = new TaxonNode((Node) result.get(QUERY_PARAM_SOURCE_TAXON));
        List<Taxon> sTaxa = RollUpDistinctTaxa.expandTaxon(sourceTaxon);
        String relationshipType = (String) result.get(QUERY_PARAM_INTERACTION_TYPE);
        Wrappers.SeqWrapper<Node> targetTaxa = (Wrappers.SeqWrapper<Node>) result.get(QUERY_PARAM_TARGET_TAXA);
        for (Node targetTaxon : targetTaxa) {
            List<Taxon> tTaxa = RollUpDistinctTaxa.expandTaxon(new TaxonNode(targetTaxon));
            for (Taxon sTaxon : sTaxa) {
                String occSourceId = sourceOccurrenceId(externalIdOrId(study), relationshipType, sTaxon);
                occIds.put(fullSourceOccurrenceId(occSourceId), sTaxon.getExternalId());
                for (Taxon tTaxon : tTaxa) {
                    occIds.put(fullTargetOccurrenceId(occSourceId, tTaxon), tTaxon.getExternalId());
                }
            }
        }
    }

    protected static String fullSourceOccurrenceId(String occSourceId) {
        return GLOBI_OCCUR_RSOURCE + occSourceId;
    }

    protected static String fullTargetOccurrenceId(String occSourceId, Taxon tTaxon) {
        return GLOBI_OCCUR_RTARGET + occSourceId + "-" + RollUpAssociations.fillBlanks(tTaxon.getExternalId());
    }

    protected static String sourceOccurrenceId(String studyId, String relationshipType, Taxon sTaxon) {
        return studyId + "-" + RollUpAssociations.fillBlanks(sTaxon.getExternalId()) + "-" + relationshipType;
    }

    protected HTreeMap<String, String> createMap() {
        return DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make()
                .createHashMap("occIds")
                .make();
    }

}
