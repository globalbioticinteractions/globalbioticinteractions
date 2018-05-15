package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ExternalIdUtil;

import java.util.HashMap;
import java.util.Map;

public abstract class ExporterTaxa extends ExporterBase {

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
        String infoURL = ExternalIdUtil.urlForExternalId(taxonId);
        if (infoURL != null) {
            properties.put(EOLDictionary.FURTHER_INFORMATION_URL, infoURL);
        }

        properties.put(EOLDictionary.SCIENTIFIC_NAME, (String) result.get("scientificName"));
        if (result.containsKey("rank")) {
            String rank = (String) result.get("rank");
            // see https://github.com/jhpoelen/eol-globi-data/issues/114
            properties.put(EOLDictionary.TAXON_RANK, StringUtils.replace(StringUtils.lowerCase(rank), "infraspecies", "Subspecies"));
        }

        addHigherOrderTaxa(properties, result, rankMap);
    }

    private static void addHigherOrderTaxa(Map<String, String> properties, Map<String, Object> result, Map<String, String> rankMap) {
        if (result.containsKey("pathNames")) {
            String[] names = StringUtils.splitPreserveAllTokens((String) result.get("pathNames"), "|");
            if (result.containsKey("path")) {
                String[] values = StringUtils.splitPreserveAllTokens((String) result.get("path"), "|");
                if (values != null && names != null && values.length == names.length) {
                    for (int i = 0; i < values.length; i++) {
                        String colName = rankMap.get(StringUtils.lowerCase(StringUtils.trim(names[i])));
                        if (colName != null) {
                            properties.put(colName, StringUtils.trim(values[i]));
                        }
                    }
                }
            }
        }
    }

    @Override
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


}
