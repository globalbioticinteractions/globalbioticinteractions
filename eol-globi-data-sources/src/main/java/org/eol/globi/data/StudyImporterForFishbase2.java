package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonImpl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForFishbase2 extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFishbase2.class);

    public static final String PREFIX_FISHBASE_COUNTRY = "FISHBASE_COUNTRY:";
    public static final String PREFIX_FISHBASE_REFERENCE = "FISHBASE_REFERENCE:";
    public static final String PREFIX_FISHBASE_TAXON = "FISHBASE:";

    public StudyImporterForFishbase2(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static Map<String, String> parsePredatorPrey(JsonNode predats) {
        Map<String, String> predatsMap = new TreeMap<String, String>();
        map(predatsMap, predats, "Predatcode", StudyImporterForTSV.SOURCE_TAXON_ID, PREFIX_FISHBASE_TAXON);
        map(predatsMap, predats, "Predatstage", StudyImporterForTSV.SOURCE_TAXON_ID, null);
        predatsMap.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.PREYS_UPON.getIRI());
        predatsMap.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, "preysOn");
        map(predatsMap, predats, "SpecCode", StudyImporterForTSV.TARGET_TAXON_ID, PREFIX_FISHBASE_TAXON);
        map(predatsMap, predats, "PreyStage", StudyImporterForTSV.TARGET_TAXON_ID, null);
        map(predatsMap, predats, "C_Code", StudyImporterForTSV.LOCALITY_ID, PREFIX_FISHBASE_COUNTRY);
        map(predatsMap, predats, "Locality", StudyImporterForTSV.LOCALITY_NAME, null);
        map(predatsMap, predats, "PredatsRefNo", StudyImporterForTSV.REFERENCE_ID, PREFIX_FISHBASE_REFERENCE);
        return predatsMap;
    }

    protected static void map(Map<String, String> predatsMap, JsonNode predats, String fromFieldName, String toFieldName, String prefix) {
        if (predats.has(fromFieldName)) {
            JsonNode specCode = predats.get(fromFieldName);
            if (!specCode.isNull()) {
                String value = specCode.asText();
                if (org.apache.commons.lang.StringUtils.isNotBlank(prefix)) {
                    value = prefix + value;
                }
                predatsMap.put(toFieldName, value);
            }
        }
    }

    public static Map<String, String> parseCountry(JsonNode jsonNode) {
        Map<String, String> country = new TreeMap<String, String>();
        map(country, jsonNode, "LatDeg", StudyImporterForTSV.DECIMAL_LATITUDE, null);
        map(country, jsonNode, "LongDeg", StudyImporterForTSV.DECIMAL_LONGITUDE, null);
        map(country, jsonNode, "C_Code", StudyImporterForTSV.LOCALITY_ID, PREFIX_FISHBASE_COUNTRY);
        return country;
    }

    protected static TaxonImpl parseTaxon(JsonNode jsonNode) {
        TaxonImpl taxon = new TaxonImpl(jsonNode.get("Genus").asText() + " " + jsonNode.get("Species").asText(), "FISHBASE:" + jsonNode.get("SpecCode").asText());
        taxon.setCommonNames(jsonNode.get("FBname").asText() + " @en");
        return taxon;
    }

    protected static Map<String, String> parseFoodItem(JsonNode foodItem) {
        Map<String, String> foodItemMap = new TreeMap<String, String>();
        foodItemMap.put(StudyImporterForTSV.SOURCE_TAXON_ID, PREFIX_FISHBASE_TAXON + foodItem.get("SpecCode").asText());
        foodItemMap.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, foodItem.get("PredatorStage").asText());
        foodItemMap.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        foodItemMap.put(StudyImporterForTSV.TARGET_LIFE_STAGE, foodItem.get("PreyStage").asText());
        String targetTaxonId = "";
        String targetTaxonName = "";
        if (foodItem.has("PreySpecCode")) {
            JsonNode value = foodItem.get("PreySpecCode");
            if (!value.isNull()) {
                targetTaxonId = "FISHBASE:" + value.asText();
            }
        } else if (foodItem.has("PreySpecCodeSLB")) {
            JsonNode value = foodItem.get("PreySpecCodeSLB");
            if (!value.isNull()) {
                targetTaxonId = "SEALIFEBASE:" + value.asText();
            }
        }

        if (org.apache.commons.lang.StringUtils.isBlank(targetTaxonId)) {
            if (foodItem.has("Foodgroup")) {
                JsonNode group = foodItem.get("Foodgroup");
                if (!group.isNull()) {
                    targetTaxonName += group.asText();
                }
            }
            if (foodItem.has("Foodname")) {
                JsonNode foodName = foodItem.get("Foodname");
                if (!foodName.isNull()) {
                    if (org.apache.commons.lang.StringUtils.isNotBlank(targetTaxonName)) {
                        targetTaxonName += " ";
                    }
                    targetTaxonName += foodName.asText();
                }
            }
        }
        if (org.apache.commons.lang.StringUtils.isNotBlank(targetTaxonId)) {
            foodItemMap.put(StudyImporterForTSV.TARGET_TAXON_ID, targetTaxonId);
        }
        if (org.apache.commons.lang.StringUtils.isNotBlank(targetTaxonName)) {
            foodItemMap.put(StudyImporterForTSV.TARGET_TAXON_NAME, targetTaxonName);
        }

        map(foodItemMap, foodItem, "FoodsRefNo", StudyImporterForTSV.REFERENCE_ID, PREFIX_FISHBASE_REFERENCE);
        map(foodItemMap, foodItem, "Locality", StudyImporterForTSV.LOCALITY_NAME, null);
        return foodItemMap;
    }

    public static Map<String, String> parseReference(JsonNode jsonNode) {
        Map<String, String> reference = new TreeMap<String, String>();
        if (jsonNode.has("RefNo")) {
            reference.put(StudyImporterForTSV.REFERENCE_ID, PREFIX_FISHBASE_REFERENCE + jsonNode.get("RefNo").asText());
        }
        map(reference, jsonNode, "DOI", StudyImporterForTSV.REFERENCE_DOI, null);

        List<String> citation = new ArrayList<String>();

        addIfExists(jsonNode, citation, "Author");
        addIfExists(jsonNode, citation, "Year");
        addIfExists(jsonNode, citation, "Title");
        addIfExists(jsonNode, citation, "Source");
        addIfExists(jsonNode, citation, "DOI");
        reference.put(StudyImporterForTSV.REFERENCE_CITATION, StringUtils.join(citation, ". ").trim());
        return reference;
    }

    protected static void addIfExists(JsonNode jsonNode, List<String> citation, String fieldName) {
        if (jsonNode.has(fieldName)) {
            JsonNode fieldValue = jsonNode.get(fieldName);
            if (!fieldValue.isNull()) {
                citation.add(fieldValue.asText());
            }
        }
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Map<String, ParserFood> config = new HashMap<String, ParserFood>() {
            {
                put("fooditems", new ParserFood());
                put("predats", new ParserPredatorPrey());
            }
        };
        for (Map.Entry<String, ParserFood> endpointParser : config.entrySet()) {
            int returned;
            int requested = 100;
            int totalReturned = 0;
            String uri = "";
            try {
                do {
                    uri = "http://fishbase.ropensci.org/" + endpointParser.getKey() + "?limit=" + requested + "&offset=" + totalReturned;
                    LOG.info("downloading [" + uri + "] in progress...");
                    JsonNode items = new ObjectMapper().readTree(new URL(uri));
                    if (items.has("data")) {
                        for (JsonNode item : items.get("data")) {
                            endpointParser.getValue().parse(item);
                        }
                    }
                    returned = items.get("returned").asInt();
                    totalReturned += returned;
                    LOG.info("downloading [" + uri + "] done.");
                } while (returned == requested);
            } catch (IOException e) {
                throw new StudyImporterException("failed to retrieve fishbase from [" + uri + "]", e);
            }
        }
        return null;
    }

    private class ParserFood {
        void parse(JsonNode node) {
            parseFoodItem(node);
        }
    }

    private class ParserPredatorPrey extends ParserFood {
        void parse(JsonNode node) {
            parsePredatorPrey(node);
        }
    }


}
