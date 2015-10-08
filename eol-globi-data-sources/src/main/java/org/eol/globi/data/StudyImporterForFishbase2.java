package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.ResourceUtil;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForFishbase2 extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFishbase2.class);
    public static final int BATCH_SIZE = 2000;

    public StudyImporterForFishbase2(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static Map<String, String> parsePredatorPrey(JsonNode predats, String prefix) {
        Map<String, String> predatsMap = new TreeMap<String, String>();
        if (predats.has("PredatcodeDB")) {
            JsonNode specCode = predats.get("PredatcodeDB");
            if (!specCode.isNull()) {
                prefix = specCode.asText();
            }
        }
        map(predatsMap, predats, "Predatcode", StudyImporterForTSV.SOURCE_TAXON_ID, prefix);
        map(predatsMap, predats, "Predatstage", StudyImporterForTSV.SOURCE_LIFE_STAGE, null);
        predatsMap.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.PREYS_UPON.getIRI());
        predatsMap.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, "preysOn");
        map(predatsMap, predats, "SpecCode", StudyImporterForTSV.TARGET_TAXON_ID, prefix);
        map(predatsMap, predats, "PreyStage", StudyImporterForTSV.TARGET_LIFE_STAGE, null);
        map(predatsMap, predats, "C_Code", StudyImporterForTSV.LOCALITY_ID, null);
        map(predatsMap, predats, "Locality", StudyImporterForTSV.LOCALITY_NAME, null);
        map(predatsMap, predats, "PredatsRefNo", StudyImporterForTSV.REFERENCE_ID, prefix + "REF:");
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
        map(country, jsonNode, "C_Code", StudyImporterForTSV.LOCALITY_ID, null);
        return country;
    }

    protected static Taxon parseTaxon(JsonNode jsonNode, String namespace) {
        TaxonImpl taxon = new TaxonImpl(jsonNode.get("Genus").asText() + " " + jsonNode.get("Species").asText(), namespace + jsonNode.get("SpecCode").asText());
        taxon.setCommonNames(jsonNode.get("FBname").asText() + " @en");
        return taxon;
    }

    protected static Map<String, String> parseFoodItem(JsonNode foodItem, String prefix) {
        Map<String, String> foodItemMap = new TreeMap<String, String>();
        foodItemMap.put(StudyImporterForTSV.SOURCE_TAXON_ID, prefix + foodItem.get("SpecCode").asText());
        foodItemMap.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, foodItem.get("PredatorStage").asText());
        foodItemMap.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        foodItemMap.put(StudyImporterForTSV.TARGET_LIFE_STAGE, foodItem.get("PreyStage").asText());
        String targetTaxonId = "";
        String targetTaxonName = "";
        if (foodItem.has("PreySpecCode")) {
            JsonNode value = foodItem.get("PreySpecCode");
            if (!value.isNull()) {
                targetTaxonId = value.asText();
            }
        }
        if (foodItem.has("PreySpecCodeDB")) {
            JsonNode value = foodItem.get("PreySpecCodeDB");
            if (!value.isNull()) {
                targetTaxonId = value.asText() + ":" + targetTaxonId;
            }
        } else if (StringUtils.isNotBlank(targetTaxonId)) {
            targetTaxonId = prefix + targetTaxonId;
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

        map(foodItemMap, foodItem, "FoodsRefNo", StudyImporterForTSV.REFERENCE_ID, prefix + "REF:");
        map(foodItemMap, foodItem, "Locality", StudyImporterForTSV.LOCALITY_NAME, null);
        return foodItemMap;
    }

    public static Map<String, String> parseReference(JsonNode jsonNode, String namespace) {
        Map<String, String> reference = new TreeMap<String, String>();
        if (jsonNode.has("RefNo")) {
            reference.put(StudyImporterForTSV.REFERENCE_ID, namespace + jsonNode.get("RefNo").asText());
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
        InteractionListenerNeo4j listener = new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger());
        importStudy(listener);
        return null;
    }

    protected void importStudy(InteractionListener listener) throws StudyImporterException {
        final References references = new References();
        final Species species = new Species();

        FishBaseConfig config = new FishBaseConfig();
        String currentYear = new DateTime().toString("YYYY");
        config.setURL("http://fishbase.ropensci.org/");
        config.setCitation("Froese, R. and D. Pauly. Editors. " + currentYear + ". FishBase. " +
                "World Wide Web electronic publication." +
                "www.fishbase.org . " + ReferenceUtil.createLastAccessedString(config.getURL()));
        config.setNamespace("FB:");
        config.setReferences(references);
        config.setSpecies(species);


        FishBaseConfig slbConfig = new FishBaseConfig();
        slbConfig.setURL("http://fishbase.ropensci.org/sealifebase/");
        slbConfig.setCitation("Froese, R. and D. Pauly. Editors. " + currentYear + ". SeaLifeBase. " +
                "World Wide Web electronic publication." +
                "www.sealifebase.org . " + ReferenceUtil.createLastAccessedString(slbConfig.getURL()));
        slbConfig.setNamespace("SLB:");
        slbConfig.setReferences(references);
        slbConfig.setSpecies(species);

        FishBaseConfig[] configs = new FishBaseConfig[]{config, slbConfig};

        for (FishBaseConfig fishBaseConfig : configs) {
            processEndpoints(fishBaseConfig.getURL(), new TreeMap<String, NodeProcessor>() {
                {
                    put("refrens", references);
                    put("species", species);
                }
            }, fishBaseConfig.getNamespace());
        }


        for (FishBaseConfig fishBaseConfig : configs) {
            importStudy(listener, fishBaseConfig);
        }
    }

    private void importStudy(final InteractionListener listener, final FishBaseConfig config) throws StudyImporterException {

        processEndpoints(config.getURL(), new TreeMap<String, NodeProcessor>() {
            {
                put("fooditems", new ParserFood(listener, sourceCitation, config.getReferences(), config.getSpecies()));
                put("predats", new ParserPredatorPrey(listener, sourceCitation, config.getReferences(), config.getSpecies()));
            }
        }, config.getNamespace());
    }

    private void processEndpoints(String baseUrl, Map<String, NodeProcessor> endpointConfig, String namespace) throws StudyImporterException {
        for (Map.Entry<String, NodeProcessor> endpointParser : endpointConfig.entrySet()) {
            int returned;
            int requested = BATCH_SIZE;
            int totalReturned = 0;
            String uri = "";
            try {
                do {
                    uri = baseUrl + endpointParser.getKey() + "?limit=" + requested + "&offset=" + totalReturned;
                    JsonNode items = new ObjectMapper().readTree(ResourceUtil.asInputStream(uri, getClass()));
                    if (items.has("data")) {
                        for (JsonNode item : items.get("data")) {
                            endpointParser.getValue().process(item, namespace);
                        }
                    }
                    returned = items.get("returned").asInt();
                    totalReturned += returned;
                } while (returned == requested);
            } catch (IOException e) {
                throw new StudyImporterException("failed to retrieve fishbase from [" + uri + "]", e);
            }
        }
    }


    private abstract class NodeProcessor {
        abstract void process(JsonNode node, String namespace) throws StudyImporterException;
    }

    private class ParserFood extends NodeProcessor {
        private InteractionListener listener;
        private String sourceCitation;
        private References references;
        private Species species;

        public ParserFood(InteractionListener listener, String sourceCitation, References references, Species species) {
            this.listener = listener;
            this.sourceCitation = sourceCitation;
            this.references = references;
            this.species = species;
        }

        Map<String, String> parse(JsonNode node, String namespace) {
            return parseFoodItem(node, namespace);
        }

        void process(JsonNode node, String namespace) throws StudyImporterException {
            Map<String, String> link = parse(node, namespace);

            addTaxonName(link, StudyImporterForTSV.SOURCE_TAXON_ID, StudyImporterForTSV.SOURCE_TAXON_NAME);
            addTaxonName(link, StudyImporterForTSV.TARGET_TAXON_ID, StudyImporterForTSV.TARGET_TAXON_NAME);

            String referenceId = link.get(StudyImporterForTSV.REFERENCE_ID);
            Map<String, String> reference = references.referenceForId(referenceId);
            if (reference != null) {
                link.putAll(reference);
            }

            link.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, sourceCitation);
            listener.newLink(link);
        }

        private void addTaxonName(Map<String, String> link, String taxonIdLabel, String taxonNameLabel) {
            String taxonId = link.get(taxonIdLabel);
            Taxon taxon = species.taxonForId(taxonId);
            if (taxon == null) {
                LOG.warn("no taxon for taxon id [" + taxonId + "]");
            } else {
                link.put(taxonNameLabel, taxon.getName());
            }
        }
    }

    private class ParserPredatorPrey extends ParserFood {
        public ParserPredatorPrey(InteractionListener listener, String sourceCitation, References references, Species species) {
            super(listener, sourceCitation, references, species);
        }


        Map<String, String> parse(JsonNode node, String namespace) {
            return parsePredatorPrey(node, namespace);
        }
    }


    private class References extends NodeProcessor {
        private Map<String, Map<String, String>> references = new TreeMap<String, Map<String, String>>();

        @Override
        void process(JsonNode node, String namespace) throws StudyImporterException {
            Map<String, String> reference = parseReference(node, namespace);
            references.put(reference.get(StudyImporterForTSV.REFERENCE_ID), reference);
        }

        Map<String, String> referenceForId(String referenceId) {
            return references.get(referenceId);
        }

    }

    private class Species extends NodeProcessor {
        private Map<String, Taxon> taxa = new TreeMap<String, Taxon>();

        @Override
        void process(JsonNode node, String namespace) throws StudyImporterException {
            Taxon taxon = parseTaxon(node, namespace);
            taxa.put(taxon.getExternalId(), taxon);
        }

        Taxon taxonForId(String externalId) {
            return externalId == null ? null : taxa.get(externalId);
        }
    }

    private class FishBaseConfig {
        private String URL;
        private String citation;
        private String namespace;
        private References references;
        private Species species;

        public void setURL(String URL) {
            this.URL = URL;
        }

        public String getURL() {
            return URL;
        }

        public void setCitation(String citation) {
            this.citation = citation;
        }

        public String getCitation() {
            return citation;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setReferences(References references) {
            this.references = references;
        }

        public References getReferences() {
            return references;
        }

        public void setSpecies(Species species) {
            this.species = species;
        }

        public Species getSpecies() {
            return species;
        }
    }
}
