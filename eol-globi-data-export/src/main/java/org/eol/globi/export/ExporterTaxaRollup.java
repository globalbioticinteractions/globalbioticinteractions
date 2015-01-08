package org.eol.globi.export;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports an expanded taxon list. Rolls up to taxonomic rank "order".
 * <p/>
 * see https://github.com/jhpoelen/eol-globi-data/issues/79
 */
public class ExporterTaxaRollup extends ExporterTaxa {

    static final List<String> EXPANDABLE_RANKS = Arrays.asList("subspecies", "species"
            , "subgenus", "genus"
            , "subtribe", "tribe"
            , "subfamily", "family", "superfamily"
            , "suborder", "order");

    public static List<Map<String, Object>> expandTaxonResult(Map<String, Object> result) {
        List<Map<String, Object>> expandedResults = new ArrayList<Map<String, Object>>();

        String[] pathNames = StringUtils.split((String) result.get("pathNames"), CharsetConstant.SEPARATOR_CHAR);
        String[] pathIds = StringUtils.split((String) result.get("pathIds"), "|");
        String[] path = StringUtils.split((String) result.get("path"), "|");
        if (pathNames != null && pathIds != null && path != null
                && pathNames.length == pathIds.length && pathNames.length == path.length) {
            for (int i = 0; i < pathNames.length; i++) {
                String rank = StringUtils.trim(pathNames[i]);
                if (EXPANDABLE_RANKS.contains(rank)) {
                    Map<String, Object> taxon = new HashMap<String, Object>();
                    taxon.put("scientificName", StringUtils.trim(path[i]));
                    taxon.put("taxonId", StringUtils.trim(pathIds[i]));
                    taxon.put("rank", rank);
                    String[] partialPathNames = Arrays.copyOfRange(pathNames, 0, i + 1);
                    String[] partialPath = Arrays.copyOfRange(path, 0, i + 1);
                    taxon.put("path", StringUtils.trim(StringUtils.join(partialPath, CharsetConstant.SEPARATOR_CHAR)));
                    taxon.put("pathNames", StringUtils.trim(StringUtils.join(partialPathNames, CharsetConstant.SEPARATOR_CHAR)));
                    expandedResults.add(taxon);
                }
            }
        }
        return expandedResults;
    }

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            // only write the taxa once, because they are unique across studies...
            exportRollupTaxa(writer, study.getUnderlyingNode().getGraphDatabase());
        }
    }

    private void exportRollupTaxa(Writer writer, GraphDatabaseService graphDatabase) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDatabase);
        ExecutionResult results = engine.execute("START taxon = node:taxons('*:*') " +
                "WHERE has(taxon.path) AND has(taxon.pathIds) AND has(taxon.pathNames) " +
                "AND has(taxon.externalId) AND taxon.externalId <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "AND has(taxon.name) AND taxon.name <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "RETURN distinct(taxon.path)" +
                ", taxon.name as scientificName" +
                ", taxon.path as path" +
                ", taxon.pathNames as pathNames" +
                ", taxon.pathIds as pathIds" +
                ", taxon.rank? as rank" +
                ", taxon.externalId as taxonId");

        HTreeMap<String, Map<String, Object>> taxonMap = null;
        try {
            taxonMap = buildExpandedTaxonMap(results);

            Map<String, String> row = new HashMap<String, String>();
            for (Map<String, Object> taxon : taxonMap.values()) {
                ExporterTaxa.resultsToRow(row, taxon);
                writeProperties(writer, row);
                row.clear();
            }
        } finally {
            if (null != taxonMap) {
                taxonMap.close();
            }
        }
    }

    public static HTreeMap<String, Map<String, Object>> buildExpandedTaxonMap(ExecutionResult results) {
        HTreeMap<String, Map<String, Object>> taxonMap;
        taxonMap = createTaxonMap();
        for (Map<String, Object> result : results) {
            List<Map<String, Object>> expandedTaxa = expandTaxonResult(result);
            for (Map<String, Object> taxon : expandedTaxa) {
                taxonMap.put((String) taxon.get("taxonId"), taxon);
            }
        }
        return taxonMap;
    }


    private static HTreeMap<String, Map<String, Object>> createTaxonMap() {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<String, Map<String, Object>> idLookup = db
                .createHashMap("taxonIdMap")
                .make();
        return idLookup;
    }


}
