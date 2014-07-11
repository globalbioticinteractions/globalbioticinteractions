package org.eol.globi.export;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterTaxa extends ExporterBase {

    protected String[] getFields() {
        return new String[]{
                EOLDictionary.TAXON_ID,
                EOLDictionary.SCIENTIFIC_NAME,
                EOLDictionary.PARENT_NAME_USAGE_ID,
                EOLDictionary.KINGDOM,
                EOLDictionary.PHYLUM,
                EOLDictionary.CLASS,
                EOLDictionary.ORDER,
                EOLDictionary.FAMILY,
                EOLDictionary.GENUS,
                EOLDictionary.TAXON_RANK,
                EOLDictionary.FURTHER_INFORMATION_URL,
                EOLDictionary.TAXONOMIC_STATUS,
                EOLDictionary.TAXON_REMARKS,
                EOLDictionary.NAME_PUBLISHED_IN,
                EOLDictionary.REFERENCE_ID
        };
    }

    @Override
    protected String getRowType() {
        return "http://rs.tdwg.org/dwc/terms/Taxon";
    }

    @Override
    public void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            // only write the taxa once, because they are unique across studies...
            exportAllDistinctTaxa(writer, study.getUnderlyingNode().getGraphDatabase());
        }
    }

    private void exportAllDistinctTaxa(Writer writer, GraphDatabaseService graphDatabase) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDatabase);
        ExecutionResult results = engine.execute("START taxon = node:taxons('*:*') " +
                "MATCH taxon<-[:CLASSIFIED_AS]-specimen " +
                "WHERE has(taxon.externalId) AND taxon.externalId <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "AND has(taxon.name) AND taxon.name <> '" + PropertyAndValueDictionary.NO_MATCH + "' " +
                "RETURN distinct(taxon)" +
                ", taxon.name as scientificName" +
                ", taxon.path? as path" +
                ", taxon.pathNames? as pathNames" +
                ", taxon.rank? as rank" +
                ", taxon.externalId as taxonId");

        Map<String, String> row = new HashMap<String, String>();
        for (Map<String, Object> result : results) {
            resultsToRow(row, result);
            writeProperties(writer, row);
            row.clear();
        }
    }

    static protected void resultsToRow(Map<String, String> properties, Map<String, Object> result) {
        Map<String, String> rankMap = new HashMap<String, String>() {
            {
                put("kingdom", EOLDictionary.KINGDOM);
                put("phylum", EOLDictionary.PHYLUM);
                put("class", EOLDictionary.CLASS);
                put("order", EOLDictionary.ORDER);
                put("family", EOLDictionary.FAMILY);
                put("genus", EOLDictionary.GENUS);
            }
        };

        String taxonId = (String) result.get("taxonId");

        properties.put(EOLDictionary.TAXON_ID, taxonId);
        String infoURL = ExternalIdUtil.infoURLForExternalId(taxonId);
        if (infoURL != null) {
            properties.put(EOLDictionary.FURTHER_INFORMATION_URL, infoURL);
        }

        properties.put(EOLDictionary.SCIENTIFIC_NAME, (String) result.get("scientificName"));
        if (result.containsKey("rank")) {
            properties.put(EOLDictionary.TAXON_RANK, (String) result.get("rank"));
        }

        addHigherOrderTaxa(properties, result, rankMap);
    }

    private static void addHigherOrderTaxa(Map<String, String> properties, Map<String, Object> result, Map<String, String> rankMap) {
        if (result.containsKey("pathNames")) {
            String[] names = StringUtils.split((String) result.get("pathNames"), "|");
            if (result.containsKey("path")) {
                String[] values = StringUtils.split((String) result.get("path"), "|");
                if (values != null && names != null && values.length == names.length) {
                    for (int i = 0; i < values.length; i++) {
                        String colName = rankMap.get(StringUtils.trim(names[i]));
                        if (colName != null) {
                            properties.put(colName, StringUtils.trim(values[i]));
                        }
                    }
                }
            }
        }
    }


}
