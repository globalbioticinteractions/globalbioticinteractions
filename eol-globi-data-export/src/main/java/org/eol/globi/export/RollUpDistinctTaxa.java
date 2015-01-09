package org.eol.globi.export;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
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
public class RollUpDistinctTaxa extends ExporterTaxa {

    static final List<String> EXPANDABLE_RANKS = Arrays.asList("subspecies", "species"
            , "subgenus", "genus"
            , "subtribe", "tribe"
            , "subfamily", "family", "superfamily"
            , "suborder", "order");

    public static List<Taxon> expandTaxonResult(Map<String, Object> result) {
        String pathNames1 = (String) result.get("pathNames");
        String pathIds1 = (String) result.get("pathIds");
        String path1 = (String) result.get("path");
        return expandTaxon(pathNames1, pathIds1, path1);
    }

    protected static List<Taxon> expandTaxon(String pathNames1, String pathIds1, String path1) {
        String[] pathNames = StringUtils.split(pathNames1, CharsetConstant.SEPARATOR_CHAR);
        String[] pathIds = StringUtils.split(pathIds1, CharsetConstant.SEPARATOR_CHAR);
        String[] path = StringUtils.split(path1, CharsetConstant.SEPARATOR_CHAR);
        return expandTaxon(pathNames, pathIds, path);
    }

    protected static List<Taxon> expandTaxon(Taxon sourceTaxon) {
        return RollUpDistinctTaxa.expandTaxon(sourceTaxon.getPathNames(),
                sourceTaxon.getPathIds(),
                sourceTaxon.getPath());
    }


    public static List<Taxon> expandTaxon(String[] pathNames, String[] pathIds, String[] path) {
        List<Taxon> expandedResults = new ArrayList<Taxon>();
        if (pathNames != null && pathIds != null && path != null
                && pathNames.length == pathIds.length && pathNames.length == path.length) {
            for (int i = 0; i < pathNames.length; i++) {
                String rank = StringUtils.trim(pathNames[i]);
                if (EXPANDABLE_RANKS.contains(rank)) {
                    Taxon taxon = new TaxonImpl();
                    taxon.setName(StringUtils.trim(path[i]));
                    taxon.setRank(rank);
                    taxon.setExternalId(StringUtils.trim(pathIds[i]));
                    String[] partialPathNames = Arrays.copyOfRange(pathNames, 0, i + 1);
                    String[] partialPath = Arrays.copyOfRange(path, 0, i + 1);
                    taxon.setPath(StringUtils.trim(StringUtils.join(partialPath, CharsetConstant.SEPARATOR_CHAR)));
                    taxon.setPathNames(StringUtils.trim(StringUtils.join(partialPathNames, CharsetConstant.SEPARATOR_CHAR)));
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
        HTreeMap<String, Map<String, Object>> taxonMap = createHTreeMap("taxonIdMap");
        for (Map<String, Object> result : results) {
            List<Taxon> taxa = expandTaxonResult(result);
            for (Taxon taxon : taxa) {
                Map<String, Object> taxonEntry = new HashMap<String, Object>();
                taxonEntry.put("scientificName", taxon.getName());
                taxonEntry.put("taxonId", taxon.getExternalId());
                taxonEntry.put("rank", taxon.getRank());
                taxonEntry.put("path", taxon.getPath());
                taxonEntry.put("pathNames", taxon.getPathNames());
                taxonMap.put(taxon.getExternalId(), taxonEntry);
            }
        }
        return taxonMap;
    }


    public static HTreeMap<String, Map<String, Object>> createHTreeMap(String mapName) {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<String, Map<String, Object>> idLookup = db
                .createHashMap(mapName)
                .make();
        return idLookup;
    }


}
